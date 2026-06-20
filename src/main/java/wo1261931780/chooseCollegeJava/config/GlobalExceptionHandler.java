package wo1261931780.chooseCollegeJava.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 6 类必备异常 + 业务异常,所有 4xx 异常精确归类,5xx 兜底不泄露 e.getMessage。
 * 跟 JOSP-yuq / JOSP-easyHotman / JOSP-devDashboard 等项目保持一致。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/** 业务异常:服务层用 throw new RuntimeException("消息") + ErrorCode 判断 */
	@ExceptionHandler(BusinessException.class)
	public ShowResult<String> handleBusinessException(BusinessException e) {
		log.warn("业务异常: code={} msg={}", e.getCode(), e.getMsg());
		ShowResult<String> r = new ShowResult<>();
		r.setCode(e.getCode());
		r.setMsg(e.getMsg());
		return r;
	}

	/**
	 * 处理参数校验异常（@Valid DTO 字段）
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ShowResult<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.collect(Collectors.joining(", "));
		log.warn("参数校验失败: {}", message);
		return ShowResult.sendError(message);
	}

	/**
	 * 处理 @RequestParam / @PathVariable 校验失败
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ShowResult<String> handleConstraintViolationException(ConstraintViolationException e) {
		String message = e.getConstraintViolations().stream()
				.map(ConstraintViolation::getMessage)
				.collect(Collectors.joining(", "));
		log.warn("参数校验失败: {}", message);
		return ShowResult.sendError(message);
	}

	/**
	 * 必填参数缺失 (?page= 漏传)
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ShowResult<String> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
		String message = "缺少必填参数: " + e.getParameterName();
		log.warn(message);
		return ShowResult.sendError(message);
	}

	/**
	 * 参数类型转换失败 (?page=abc 期望 int)
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ShowResult<String> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
		String message = "参数类型错误: " + e.getName() + " 应为 " +
				(e.getRequiredType() == null ? "未知" : e.getRequiredType().getSimpleName());
		log.warn(message);
		return ShowResult.sendError(message);
	}

	/**
	 * 业务代码内 Long.parseLong / Integer.parseInt 越界或格式错误
	 */
	@ExceptionHandler(NumberFormatException.class)
	public ShowResult<String> handleNumberFormatException(NumberFormatException e) {
		log.warn("数字格式错误: {}", e.getMessage());
		return ShowResult.sendError("参数格式错误: 请检查数字字段");
	}

	/**
	 * 请求方法不被支持 (GET/POST/PUT/DELETE 错用)
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ShowResult<String> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
		String message = "请求方法不被支持: " + e.getMethod();
		log.warn(message);
		return ShowResult.sendError(message);
	}

	/**
	 * Spring 6.x 访问不存在的静态资源路径,静默当 404 而不是 500
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ShowResult<String> handleNoResourceFoundException(NoResourceFoundException e) {
		String message = "资源不存在: " + e.getResourcePath();
		log.warn(message);
		return ShowResult.sendError(message);
	}

	/**
	 * 上传文件超出限制
	 */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ShowResult<String> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
		String message = "上传文件过大,最大允许 " + e.getMaxUploadSize() + " 字节";
		log.warn(message);
		return ShowResult.sendError(message);
	}

	/**
	 * 请求体无法解析 (JSON 格式错误)
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ShowResult<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
		log.warn("请求体解析失败: {}", e.getMessage());
		return ShowResult.sendError("请求体格式错误,请检查 JSON 格式");
	}

	/**
	 * 处理运行时异常（兜底,会泄露实现,详细堆栈走 server log）
	 */
	@ExceptionHandler(RuntimeException.class)
	public ShowResult<String> handleRuntimeException(RuntimeException e) {
		log.error("运行时异常: ", e);
		return ShowResult.sendError("服务器内部错误, 请稍后重试");
	}

	/**
	 * 处理所有异常（最终兜底）
	 */
	@ExceptionHandler(Exception.class)
	public ShowResult<String> handleException(Exception e) {
		log.error("系统异常: ", e);
		return ShowResult.sendError("服务器内部错误, 请稍后重试");
	}
}