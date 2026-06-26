package com.choosephd.common;

/**
 * 业务异常 — service 层抛出，由 {@link GlobalExceptionHandler} 接住返 4xx。
 *
 * <p>使用场景：
 * <ul>
 *   <li>单参数构造：消息 + 默认 code=400 (通用 bad request)</li>
 *   <li>双参数构造：自定义 code + message (如 404 NOT_FOUND, 409 CONFLICT)</li>
 * </ul>
 *
 * <p>跟 RuntimeException 区别：本异常会触发 GlobalExceptionHandler
 * 走 4xx 映射；RuntimeException 走 500 兜底。前端展示消息更友好。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
