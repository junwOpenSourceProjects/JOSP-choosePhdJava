package com.choosephd.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器 — 把 Spring MVC 抛出的各类异常映射为统一 {@link ApiResult} 格式，
 * 避免泄露内部异常信息或堆栈给前端。
 *
 * <p>覆盖的异常类型：
 * <ul>
 *   <li>{@link BusinessException} — 业务异常，HTTP 400</li>
 *   <li>{@link MethodArgumentNotValidException} — @Valid DTO 字段校验失败，HTTP 400</li>
 *   <li>{@link BindException} — form 数据绑定失败，HTTP 400</li>
 *   <li>{@link ConstraintViolationException} — @Validated 单参数校验失败，HTTP 400</li>
 *   <li>{@link MissingServletRequestParameterException} — 必填 query 缺失，HTTP 400</li>
 *   <li>{@link MethodArgumentTypeMismatchException} — 类型转换失败（如 ?page=abc），HTTP 400</li>
 *   <li>{@link NumberFormatException} — 业务代码 Long.parseLong 失败，HTTP 400</li>
 *   <li>{@link HttpMessageNotReadableException} — 请求体 JSON 解析失败，HTTP 400</li>
 *   <li>{@link HttpRequestMethodNotSupportedException} — 405 Method Not Allowed</li>
 *   <li>{@link NoResourceFoundException} — 静态资源/不存在路径，HTTP 404</li>
 *   <li>{@link AccessDeniedException} — 鉴权失败，HTTP 403</li>
 *   <li>{@link Exception} — 兜底，HTTP 500，不泄露内部消息</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleBusiness(BusinessException e) {
        log.warn("Business error: {}", e.getMessage());
        return ApiResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", msg);
        return ApiResult.error(400, msg.isEmpty() ? "请求参数校验失败" : msg);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Bind failed: {}", msg);
        return ApiResult.error(400, msg.isEmpty() ? "请求参数绑定失败" : msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {}", msg);
        return ApiResult.error(400, msg.isEmpty() ? "请求参数不合法" : msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("Missing required param: {}", e.getParameterName());
        return ApiResult.error(400, "缺少必填参数: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch: param={} value={}", e.getName(), e.getValue());
        return ApiResult.error(400, "参数类型错误: " + e.getName());
    }

    @ExceptionHandler(NumberFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleNumberFormat(NumberFormatException e) {
        log.warn("Number format error: {}", e.getMessage());
        return ApiResult.error(400, "数字格式错误");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("Request body not readable: {}", e.getMessage());
        return ApiResult.error(400, "请求体格式错误");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResult<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not supported: {}", e.getMessage());
        return ApiResult.error(405, "HTTP 方法不支持: " + e.getMethod());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResult<Void> handleNoResource(NoResourceFoundException e) {
        log.warn("Resource not found: {}", e.getResourcePath());
        return ApiResult.error(404, "资源不存在");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResult<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return ApiResult.error(403, "无权限访问");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleException(Exception e) {
        log.error("Unexpected error", e);
        // 兜底不泄露内部实现/SQL/文件路径，仅返通用消息，详细堆栈走 server log
        return ApiResult.error(500, "服务器内部错误，请稍后重试");
    }
}