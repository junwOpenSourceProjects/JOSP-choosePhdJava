package wo1261931780.chooseCollegeJava.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
 * Created by Intellij IDEA.
 * Project:JOSP-BillDesktopJava
 * Package:wo1261931780.JOSPBillDesktopJava.config
 *
 * @author liujiajun_junw
 * @Date 2024-02-10-02  星期二
 * @Description
 */
public class CommonTool {
	//参数1为终端ID
//参数2为数据中心ID
	public static Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);
	// long NEXT_ID = snowflake.nextId();
}
