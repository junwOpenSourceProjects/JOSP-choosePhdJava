package wo1261931780.chooseCollegeJava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;
import wo1261931780.chooseCollegeJava.service.RankDataImportService;

/**
 * 排名数据导入服务实现
 * <p>TSV 格式: 7 列 (大学名称 \\t 大学英文 \\t 大学标签(国家, 洲) \\t 排名名称 \\t 年份 \\t 当前排名(取整) \\t 当前排名(原始))
 * <p>幂等: 同一 (universityNameChinese, rankingYear, rankVariant) 三元组 delete-再-insert, 整批 in-clause 一次性删除
 */
@Slf4j
@Service
public class RankDataImportServiceImpl implements RankDataImportService {

	private static final Pattern RANK_PATTERN = Pattern.compile("#?\\s*(\\d+)");

	@Autowired
	private UniversityRankingsQsService qsService;

	// ============ MultipartFile 入口 ============
	@Override
	public int importFromMultipartFiles(MultipartFile[] files) {
		if (files == null || files.length == 0) {
			return 0;
		}
		int total = 0;
		for (MultipartFile file : files) {
			if (file.isEmpty()) {
				continue;
			}
			try (InputStream is = file.getInputStream()) {
				total += parseAndSaveTransactional(is, file.getOriginalFilename());
			} catch (IOException e) {
				log.error("读取上传文件失败: {}", file.getOriginalFilename(), e);
				throw new RuntimeException("读取文件失败: " + file.getOriginalFilename(), e);
			}
		}
		return total;
	}

	// ============ 本地 File 入口 ============
	@Override
	public int importFromFiles(List<File> files) {
		if (files == null || files.isEmpty()) {
			return 0;
		}
		int total = 0;
		for (File file : files) {
			try (InputStream is = new FileInputStream(file)) {
				total += parseAndSaveTransactional(is, file.getName());
			} catch (IOException e) {
				log.error("读取本地文件失败: {}", file.getName(), e);
				throw new RuntimeException("读取文件失败: " + file.getName(), e);
			}
		}
		return total;
	}

	// ============ 目录扫描入口 ============
	@Override
	public ImportResult importFromDirectory(File dir) {
		if (dir == null || !dir.isDirectory()) {
			throw new IllegalArgumentException("目录不存在或不是目录: " + dir);
		}
		File[] txts = dir.listFiles((f) -> f.isFile() && f.getName().toLowerCase().endsWith(".txt"));
		if (txts == null || txts.length == 0) {
			return new ImportResult(0, 0, new ArrayList<>());
		}
		List<FileResult> results = new ArrayList<>();
		int totalRecords = 0;
		for (File f : txts) {
			try (InputStream is = new FileInputStream(f)) {
				int n = parseAndSaveTransactional(is, f.getName());
				results.add(new FileResult(f.getName(), n, "OK", ""));
				totalRecords += n;
			} catch (Exception e) {
				log.error("导入文件失败: {}", f.getName(), e);
				results.add(new FileResult(f.getName(), 0, "FAIL", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
			}
		}
		log.info("目录扫描导入完成: 目录={} 文件数={} 记录数={}", dir.getAbsolutePath(), txts.length, totalRecords);
		return new ImportResult(totalRecords, txts.length, results);
	}

	// ============ 预览入口(不入库)============
	@Override
	public PreviewResult previewFromInputStream(InputStream is, String name) throws IOException {
		String[] headers = null;
		List<String[]> sample = new ArrayList<>();
		int total = 0;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line;
			boolean first = true;
			while ((line = reader.readLine()) != null) {
				if (first) {
					first = false;
					headers = line.split("\t");
					continue;
				}
				if (line.trim().isEmpty()) {
					continue;
				}
				total++;
				if (sample.size() < 5) {
					sample.add(line.split("\t"));
				}
			}
		}
		return new PreviewResult(name == null ? "" : name, headers == null ? new String[0] : headers, sample, total);
	}

	// ============ 核心: 解析 + 幂等保存(整批一个事务)============
	@Transactional(rollbackFor = Exception.class)
	protected int parseAndSaveTransactional(InputStream is, String fileName) throws IOException {
		List<UniversityRankingsQs> list = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line;
			boolean first = true;
			while ((line = reader.readLine()) != null) {
				if (first) {
					first = false;
					continue;
				}
				if (line.trim().isEmpty()) {
					continue;
				}
				UniversityRankingsQs record = parseLine(line);
				if (record != null) {
					list.add(record);
				}
			}
		}

		if (list.isEmpty()) {
			log.info("文件 [{}] 解析为 0 条记录 (可能是空文件或列数不足), 跳过", fileName);
			return 0;
		}

		// 幂等: 按 (大学名, 年份, rankVariant) 三元组批量 delete-再-insert
		// 1) 收集不重复三元组
		Set<Triple> triples = new HashSet<>();
		for (UniversityRankingsQs r : list) {
			triples.add(new Triple(r.getUniversityNameChinese(), r.getRankingYear(), r.getRankVariant()));
		}

		// 2) 按三元组逐组删除 (用 OR in 子查询避免 N+1 写)
		int deleted = 0;
		List<Triple> tripleList = new ArrayList<>(triples);
		// 分批 (每批 50 组) 避免单 SQL 过大
		int batch = 50;
		for (int i = 0; i < tripleList.size(); i += batch) {
			List<Triple> sub = tripleList.subList(i, Math.min(i + batch, tripleList.size()));
			LambdaQueryWrapper<UniversityRankingsQs> wrapper = new LambdaQueryWrapper<>();
			wrapper.and(w -> {
				for (int j = 0; j < sub.size(); j++) {
					Triple t = sub.get(j);
					if (j > 0) {
						w.or();
					}
					w.eq(UniversityRankingsQs::getUniversityNameChinese, t.name)
							.eq(UniversityRankingsQs::getRankingYear, t.year)
							.eq(UniversityRankingsQs::getRankVariant, t.variant);
				}
			});
			deleted += qsService.count(wrapper);
			qsService.remove(wrapper);
		}

		// 3) 批量插入
		qsService.saveBatch(list);

		log.info("文件 [{}] 导入完成: 新增 {} 条, 覆盖旧数据 {} 条", fileName, list.size(), deleted);
		return list.size();
	}

	// ============ 解析单行 TSV ============
	private UniversityRankingsQs parseLine(String line) {
		String[] cols = line.split("\t");
		if (cols.length < 7) {
			log.warn("列数不足, 跳过: {}", line);
			return null;
		}

		String nameChinese = cols[0].trim();
		String nameEnglish = cols[1].trim();
		String tagStr = cols[2].trim();
		String rankName = cols[3].trim();
		String year = cols[4].trim();
		String rankIntegerStr = cols[5].trim();
		String rankRaw = cols[6].trim();

		String[] tags = Arrays.stream(tagStr.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toArray(String[]::new);
		String country = tags.length > 0 ? tags[0] : tagStr;
		String continent = tags.length > 1 ? tags[1] : "";

		Integer rankInteger = parseRankInteger(rankIntegerStr);

		UniversityRankingsQs record = new UniversityRankingsQs();
		record.setUniversityNameChinese(nameChinese);
		record.setUniversityNameEnglish(nameEnglish);
		record.setUniversityTags(country);
		record.setUniversityTagsState(continent);
		record.setRankingCategory(rankName);
		record.setRankingYear(year);
		record.setCurrentRankInteger(rankInteger);
		record.setCurrentRankRaw(rankRaw.isEmpty() ? rankIntegerStr : rankRaw);
		record.setRankVariant(rankName);
		return record;
	}

	private Integer parseRankInteger(String rankStr) {
		Matcher matcher = RANK_PATTERN.matcher(rankStr);
		if (matcher.find()) {
			try {
				return Integer.parseInt(matcher.group(1));
			} catch (NumberFormatException e) {
				log.warn("排名解析失败: {}", rankStr);
			}
		}
		return null;
	}

	/** 三元组: 唯一标识一条排名记录 */
	private record Triple(String name, String year, String variant) {}
}
