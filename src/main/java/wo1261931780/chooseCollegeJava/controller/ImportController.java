package wo1261931780.chooseCollegeJava.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.io.File;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import wo1261931780.chooseCollegeJava.config.RequireRole;
import wo1261931780.chooseCollegeJava.config.RoleConstants;
import wo1261931780.chooseCollegeJava.config.ShowResult;
import wo1261931780.chooseCollegeJava.mapper.UniversityRankingsQsMapper;
import wo1261931780.chooseCollegeJava.service.RankDataImportService;
import wo1261931780.chooseCollegeJava.service.RankDataImportService.ImportResult;
import wo1261931780.chooseCollegeJava.service.RankDataImportService.PreviewResult;

/**
 * 排名数据导入控制器
 * <p>5 个接口: 文件上传 / 目录扫描 / 文件预览 / 历史列表 / 总量统计
 */
@RestController
@RequestMapping("/import")
public class ImportController {

	@Autowired
	private RankDataImportService importService;

	@Autowired
	private UniversityRankingsQsMapper qsMapper;

	/** 应用工作目录下的 qs 排名文件夹, 用于"扫描本地目录"一键导入 */
	@Value("${app.data.rank-dir:qs 排名}")
	private String rankDir;

	/**
	 * 上传 txt 排名文件并导入数据库
	 */
	@PostMapping("/rankings")
	@RequireRole(RoleConstants.ROLE_ADMIN)
	public ShowResult<Integer> importRankings(@RequestParam("files") MultipartFile[] files) {
		int count = importService.importFromMultipartFiles(files);
		return ShowResult.sendSuccess(count);
	}

	/**
	 * 扫描应用工作目录的 qs 排名文件夹, 全部导入
	 */
	@PostMapping("/rankings/scanLocal")
	@RequireRole(RoleConstants.ROLE_ADMIN)
	public ShowResult<ImportResult> scanLocal() {
		File dir = new File(rankDir);
		ImportResult result = importService.importFromDirectory(dir);
		return ShowResult.sendSuccess(result);
	}

	/**
	 * 扫描指定绝对路径的目录
	 */
	@PostMapping("/rankings/scanPath")
	@RequireRole(RoleConstants.ROLE_ADMIN)
	public ShowResult<ImportResult> scanPath(@RequestBody Map<String, String> body) {
		String path = body == null ? null : body.get("path");
		if (path == null || path.isBlank()) {
			return ShowResult.sendError("路径不能为空");
		}
		File dir = new File(path);
		ImportResult result = importService.importFromDirectory(dir);
		return ShowResult.sendSuccess(result);
	}

	/**
	 * 预览上传文件的前 5 行 (不入库)
	 */
	@PostMapping("/rankings/preview")
	@RequireRole(RoleConstants.ROLE_ADMIN)
	public ShowResult<PreviewResult> preview(@RequestParam("file") MultipartFile file) {
		try {
			PreviewResult pr = importService.previewFromInputStream(file.getInputStream(), file.getOriginalFilename());
			return ShowResult.sendSuccess(pr);
		} catch (Exception e) {
			log.error("预览失败", e);
			return ShowResult.sendError("预览失败: " + e.getMessage());
		}
	}

	/**
	 * 历史导入记录分页 (按 ranking_year + rank_variant 分组)
	 */
	@GetMapping("/rankings/history")
	@RequireRole(RoleConstants.ROLE_ADMIN)
	public ShowResult<IPage<HistoryGroup>> history(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int pageSize,
			@RequestParam(required = false) String rankVariant) {
		List<Map<String, String>> all = qsMapper.listDistinctGroup(rankVariant);
		int total = qsMapper.countDistinctGroup(rankVariant);
		int from = Math.max(0, (page - 1) * pageSize);
		int to = Math.min(all.size(), from + pageSize);
		List<HistoryGroup> pageData = all.subList(from, to).stream()
				.map(m -> new HistoryGroup(m.get("rankVariant"), m.get("rankingYear")))
				.toList();
		IPage<HistoryGroup> result = new Page<>(page, pageSize, total);
		result.setRecords(pageData);
		return ShowResult.sendSuccess(result);
	}

	/**
	 * 导入总量统计
	 */
	@GetMapping("/rankings/stats")
	@RequireRole(RoleConstants.ROLE_ADMIN)
	public ShowResult<java.util.Map<String, Object>> stats() {
		java.util.Map<String, Object> m = new java.util.HashMap<>();
		m.put("qs_total", qsMapper.listDistinctGroup(null).size());  // 历史 group 数
		return ShowResult.sendSuccess(m);
	}

	/** 历史组: 一种排名 + 一个年份 */
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class HistoryGroup {
		private String rankVariant;
		private String rankingYear;
	}

	// log 字段
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImportController.class);
}
