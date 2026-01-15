package pvt.mktech.petcare.common.exception;

import lombok.Getter;

/**
 * 业务异常类
 * 用于处理业务逻辑中的异常情况
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final String code;
    private final String message;
    private final Object data;

    // 性能优化： 重写Throwable.fillInStackTrace() 方法，避免生成堆栈信息
//    @Override
//    public synchronized Throwable fillInStackTrace() {
//        return this;
//    }

    /**
     * 构造方法 - 使用错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.data = null;
    }
    
    /**
     * 构造方法 - 使用错误码枚举和自定义消息
     */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
        this.message = customMessage;
        this.data = null;
    }
    
    /**
     * 构造方法 - 使用错误码枚举和附加数据
     */
    public BusinessException(ErrorCode errorCode, Object data) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.data = data;
    }
    
    /**
     * 构造方法 - 使用错误码枚举、自定义消息和附加数据
     */
    public BusinessException(ErrorCode errorCode, String customMessage, Object data) {
        super(customMessage);
        this.code = errorCode.getCode();
        this.message = customMessage;
        this.data = data;
    }
    
    /**
     * 构造方法 - 直接使用错误码和消息
     */
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
        this.data = null;
    }
    
    /**
     * 构造方法 - 直接使用错误码、消息和附加数据
     */
    public BusinessException(String code, String message, Object data) {
        super(message);
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    @Override
    public String toString() {
        return "BusinessException{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
    
    /**
     * 快速创建业务异常（使用错误码枚举）
     */
    public static BusinessException of(ErrorCode errorCode) {
        return new BusinessException(errorCode);
    }
    
    /**
     * 快速创建业务异常（使用错误码枚举和自定义消息）
     */
    public static BusinessException of(ErrorCode errorCode, String customMessage) {
        return new BusinessException(errorCode, customMessage);
    }
    
    /**
     * 快速创建业务异常（使用错误码和消息）
     */
    public static BusinessException of(String code, String message) {
        return new BusinessException(code, message);
    }
}