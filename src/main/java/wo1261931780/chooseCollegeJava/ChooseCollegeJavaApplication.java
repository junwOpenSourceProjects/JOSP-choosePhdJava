package wo1261931780.chooseCollegeJava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * ChooseCollegeJava应用启动类
 */
@EnableCaching
@SpringBootApplication
public class ChooseCollegeJavaApplication {
	/**
	 * 应用入口
	 */

	public static void main(String[] args) {
		SpringApplication.run(ChooseCollegeJavaApplication.class, args);
	}

}
//todo 新建前端页面， 双曲线。
// 查询条件设置，大洲，国家，qs和usnews选择
// 自动回调搜索选项设置
// 底部展示表格，中间渲染图表先等等
// 然后，asu数据直接写死在前端