package wo1261931780.chooseCollegeJava;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import wo1261931780.chooseCollegeJava.service.RankDataImportService;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 本地 QS 排名数据导入测试
 *
 * <p>运行本测试可将项目根目录下 "qs 排名/" 中的 txt 数据导入数据库。</p>
 */
@SpringBootTest
class LocalDataImportTest {

	@Autowired
	private RankDataImportService importService;

	@Test
	void importLocalQsRankings() {
		File dataDir = new File("qs 排名");
		if (!dataDir.exists() || !dataDir.isDirectory()) {
			return;
		}
		File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".txt"));
		if (files == null || files.length == 0) {
			return;
		}
		List<File> fileList = Arrays.asList(files);
		int count = importService.importFromFiles(fileList);
		System.out.println("本地 QS 排名数据导入完成，共 " + count + " 条");
	}
}
