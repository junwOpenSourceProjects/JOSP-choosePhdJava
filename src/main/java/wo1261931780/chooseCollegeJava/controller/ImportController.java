package wo1261931780.chooseCollegeJava.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import wo1261931780.chooseCollegeJava.config.RequireRole;
import wo1261931780.chooseCollegeJava.config.RoleConstants;
import wo1261931780.chooseCollegeJava.config.ShowResult;
import wo1261931780.chooseCollegeJava.service.RankDataImportService;

/**
 * 排名数据导入控制器
 */
@RestController
@RequestMapping("/import")
public class ImportController {

	@Autowired
	private RankDataImportService importService;

	/**
	 * 上传排名 txt 文件并导入数据库
	 *
	 * @param files 排名数据文件
	 * @return 导入结果
	 */
	@PostMapping("/rankings")
	@RequireRole(RoleConstants.ROLE_ADMIN)
	public ShowResult<String> importRankings(@RequestParam("files") MultipartFile[] files) {
		int count = importService.importFromMultipartFiles(files);
		return ShowResult.sendSuccess("成功导入 " + count + " 条排名数据");
	}
}
