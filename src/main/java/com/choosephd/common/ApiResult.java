package com.choosephd.common;

/**
 * 统一 API 响应包装类 — 所有 controller 返回值的标准化格式。
 *
 * <p>字段：code / message / data / traceId（可选）。
 *
 * <p>约定：
 * <ul>
 *   <li>code=0 — 成功（用 {@code ApiResult.ok(data)}）</li>
 *   <li>code!=0 — 业务失败（消息给前端展示）</li>
 *   <li>4xx/5xx — HTTP 错误（仍包成 ApiResult 返，code 自定义）</li>
 * </ul>
 *
 * <p>前端用 {@code res.code === 0} 判断成功，{@code res.data} 取业务数据。
 */
public class ApiResult<T> {

    private int code;
    private String message;
    private T data;
    private String traceId;

    public ApiResult() {
    }

    public ApiResult(int code, String message, T data, String traceId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
    }

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "ok", data, null);
    }

    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> error(int code, String message) {
        return new ApiResult<>(code, message, null, null);
    }

    public static <T> ApiResult<T> error(String message) {
        return error(500, message);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
