package wo1261931780.chooseCollegeJava.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;
import wo1261931780.chooseCollegeJava.service.impl.UniversityRankingsQsService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 排名数据导入服务
 */
@Slf4j
@Service
public class RankDataImportService {

	private static final Pattern RANK_PATTERN = Pattern.compile("#?\\s*(\\d+)");

	@Autowired
	private UniversityRankingsQsService qsService;

	/**
	 * 从上传文件导入排名数据
	 *
	 * @param files 排名 txt 文件
	 * @return 导入记录数
	 */
	@Transactional(rollbackFor = Exception.class)
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
				total += parseAndSave(is, file.getOriginalFilename());
			} catch (IOException e) {
				log.error("读取上传文件失败: {}", file.getOriginalFilename(), e);
				throw new RuntimeException("读取文件失败: " + file.getOriginalFilename(), e);
			}
		}
		return total;
	}

	/**
	 * 从本地文件导入排名数据
	 *
	 * @param files 本地 txt 文件
	 * @return 导入记录数
	 */
	@Transactional(rollbackFor = Exception.class)
	public int importFromFiles(List<File> files) {
		if (files == null || files.isEmpty()) {
			return 0;
		}
		int total = 0;
		for (File file : files) {
			try (InputStream is = new FileInputStream(file)) {
				total += parseAndSave(is, file.getName());
			} catch (IOException e) {
				log.error("读取本地文件失败: {}", file.getName(), e);
				throw new RuntimeException("读取文件失败: " + file.getName(), e);
			}
		}
		return total;
	}

	/**
	 * 解析输入流并保存
	 *
	 * @param is       输入流
	 * @param fileName 文件名
	 * @return 导入记录数
	 * @throws IOException IO 异常
	 */
	private int parseAndSave(InputStream is, String fileName) throws IOException {
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

		for (UniversityRankingsQs record : list) {
			LambdaQueryWrapper<UniversityRankingsQs> wrapper = new LambdaQueryWrapper<>();
			wrapper.eq(UniversityRankingsQs::getUniversityNameChinese, record.getUniversityNameChinese())
					.eq(UniversityRankingsQs::getRankingYear, record.getRankingYear())
					.eq(UniversityRankingsQs::getRankVariant, record.getRankVariant());
			qsService.remove(wrapper);
		}
		qsService.saveBatch(list);
		log.info("文件 [{}] 导入 {} 条 QS 排名数据", fileName, list.size());
		return list.size();
	}

	/**
	 * 解析单行 TSV 数据
	 *
	 * @param line 行数据
	 * @return 实体对象
	 */
	private UniversityRankingsQs parseLine(String line) {
		String[] cols = line.split("\t");
		if (cols.length < 7) {
			log.warn("列数不足，跳过: {}", line);
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

	/**
	 * 解析排名整数
	 *
	 * @param rankStr 排名字符串
	 * @return 整数排名
	 */
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
}
