package pvt.mktech.petcare.common.dto.response;

import lombok.Getter;

/**
 * HTTP 状态码枚举
 * 仅用于 HTTP 响应状态码映射，不用于业务错误码
 * 业务错误码请使用 ErrorCode
 */
@Getter
public enum ResultCode {
    SUCCESS("200", "操作成功"),
    VALIDATE_FAILED("400", "参数检验失败"),
    UNAUTHORIZED("401", "暂未登录或token已经过期"),
    FORBIDDEN("403", "没有相关权限"),
    NOT_FOUND("404", "资源不存在"),
    TOO_MANY_REQUESTS("429", "请求过于频繁，请稍后再试"),
    INTERNAL_SERVER_ERROR("500", "服务器内部错误");

    private final String code;
    private final String message;

    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}