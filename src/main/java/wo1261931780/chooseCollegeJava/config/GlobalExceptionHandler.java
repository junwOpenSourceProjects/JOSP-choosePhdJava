package wo1261931780.chooseCollegeJava.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * @author junw
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理所有异常
     */
    @ExceptionHandler(Exception.class)
    public ShowResult<String> handleException(Exception e) {
        log.error("系统异常: ", e);
        return ShowResult.sendError("系统异常: " + e.getMessage());
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ShowResult<String> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: ", e);
        return ShowResult.sendError("运行时异常: " + e.getMessage());
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ShowResult<String> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常: ", e);
        return ShowResult.sendError("空指针异常: " + e.getMessage());
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ShowResult<String> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数异常: ", e);
        return ShowResult.sendError("非法参数异常: " + e.getMessage());
    }
}
