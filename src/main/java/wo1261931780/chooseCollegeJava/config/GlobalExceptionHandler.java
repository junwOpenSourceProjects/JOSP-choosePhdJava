package wo1261931780.chooseCollegeJava.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 处理参数校验异常（@Valid）
	 *
	 * @param e 校验异常
	 * @return 统一错误响应
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
	 * 处理参数校验异常（@RequestParam / @PathVariable）
	 *
	 * @param e 校验异常
	 * @return 统一错误响应
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
	 * 处理运行时异常
	 *
	 * @param e 运行时异常
	 * @return 统一错误响应
	 */
	@ExceptionHandler(RuntimeException.class)
	public ShowResult<String> handleRuntimeException(RuntimeException e) {
		log.error("运行时异常: ", e);
		return ShowResult.sendError("运行时异常: " + e.getMessage());
	}

	/**
	 * 处理所有异常
	 *
	 * @param e 异常
	 * @return 统一错误响应
	 */
	@ExceptionHandler(Exception.class)
	public ShowResult<String> handleException(Exception e) {
		log.error("系统异常: ", e);
		return ShowResult.sendError("系统异常，请稍后重试");
	}
}