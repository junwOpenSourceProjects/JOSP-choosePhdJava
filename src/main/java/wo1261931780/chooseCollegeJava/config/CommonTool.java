package wo1261931780.chooseCollegeJava.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
 * 通用工具类
 */
public class CommonTool {

	/**
	 * 雪花算法 ID 生成器
	 */
	public static Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);
}