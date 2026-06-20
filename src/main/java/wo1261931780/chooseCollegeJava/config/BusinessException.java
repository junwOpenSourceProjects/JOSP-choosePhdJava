package wo1261931780.chooseCollegeJava.config;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 服务层 throw new BusinessException(code, msg) 抛出,GlobalExceptionHandler 统一捕获
 * 避免泄露底层实现细节。
 * <p>
 * code 推荐: 40001 参数错误 / 40100 未登录 / 40300 权限不足 / 40400 资源不存在 /
 * 50000 系统错误 / 42900 请求过于频繁
 */
@Getter
public class BusinessException extends RuntimeException {

	private final Integer code;
	private final String msg;

	public BusinessException(Integer code, String msg) {
		super(msg);
		this.code = code;
		this.msg = msg;
	}

	public BusinessException(String msg) {
		super(msg);
		this.code = 50000;
		this.msg = msg;
	}
}