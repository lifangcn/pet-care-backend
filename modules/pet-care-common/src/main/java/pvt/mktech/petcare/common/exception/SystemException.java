package pvt.mktech.petcare.common.exception;

import lombok.Getter;

/**
 * 系统异常类
 * 用于处理系统级别的异常情况（如数据库异常、网络异常等）
 */
@Getter
public class SystemException extends RuntimeException {

    private final String code;
    private final String message;

    public SystemException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public SystemException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
        this.message = customMessage;
    }

    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public SystemException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.code = errorCode.getCode();
        this.message = customMessage;
    }

    public SystemException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public SystemException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    public static SystemException of(ErrorCode errorCode) {
        return new SystemException(errorCode);
    }

    public static SystemException of(ErrorCode errorCode, String customMessage) {
        return new SystemException(errorCode, customMessage);
    }

    public static SystemException of(ErrorCode errorCode, Throwable cause) {
        return new SystemException(errorCode, cause);
    }
}

