package wo1261931780.chooseCollegeJava.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author junw
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShowResult<T> {
	private Integer code;
	// 这里前端和后端拿到的变量必须一致，否则直接res.code会无法跳转

	private String msg;
	private T data;

	/**
	 * 请求成功
	 *
	 * @param object 对象
	 * @param <T>    成功消息/实体类
	 * @return 返回结果
	 */
	public static <T> ShowResult<T> sendSuccess(T object) {
		ShowResult<T> tShowResult = new ShowResult<>();
		tShowResult.setData(object);
		tShowResult.setCode(20000);
		return tShowResult;
	}

	/**
	 * 请求失败
	 *
	 * @param returnMsg 失败消息
	 * @param <T>       失败消息/实体类
	 * @return 返回结果
	 */
	public static <T> ShowResult<T> sendError(String returnMsg) {
		ShowResult<T> tShowResult = new ShowResult<>();
		tShowResult.setCode(0);
		tShowResult.setMsg(returnMsg);
		return tShowResult;
	}
}
