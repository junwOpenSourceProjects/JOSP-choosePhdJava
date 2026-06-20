package wo1261931780.chooseCollegeJava.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 排名数据导入服务
 */
public interface RankDataImportService {

	/**
	 * 从上传文件导入排名数据
	 *
	 * @param files 排名 txt 文件
	 * @return 导入记录数
	 */
	int importFromMultipartFiles(MultipartFile[] files);

	/**
	 * 从本地文件导入排名数据
	 *
	 * @param files 本地 txt 文件列表
	 * @return 导入记录数
	 */
	int importFromFiles(List<File> files);

	/**
	 * 扫描指定目录, 导入所有 .txt 排名文件
	 *
	 * @param dir 目录
	 * @return 导入结果摘要
	 */
	ImportResult importFromDirectory(File dir);

	/**
	 * 解析单个输入流(不入库), 仅用于前端预览前 5 行
	 *
	 * @param is   输入流
	 * @param name 文件名
	 * @return 预览结果
	 */
	PreviewResult previewFromInputStream(InputStream is, String name) throws IOException;

	/**
	 * 导入结果摘要
	 */
	record ImportResult(int totalRecords, int filesScanned, List<FileResult> fileResults) {}

	/**
	 * 单文件导入结果
	 */
	record FileResult(String fileName, int records, String status, String message) {}

	/**
	 * 预览结果: 前 5 行 + 字段名
	 */
	record PreviewResult(String fileName, String[] headers, List<String[]> sampleRows, int totalRows) {}
}
