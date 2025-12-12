package pvt.mktech.petcare.common.exception;

import lombok.Getter;

/**
 * 错误码枚举
 * 定义系统中所有的错误码和错误信息
 */
@Getter
public enum ErrorCode {
    
    // ========== 通用错误码 ==========
    SUCCESS("00000", "操作成功"),
    SYSTEM_ERROR("00001", "系统异常，请稍后重试"),
    PARAM_ERROR("00002", "参数错误"),
    DATA_NOT_FOUND("00003", "数据不存在"),
    DATA_ALREADY_EXISTS("00004", "数据已存在"),
    OPERATION_FAILED("00005", "操作失败"),
    UNAUTHORIZED("00006", "未授权访问"),
    FORBIDDEN("00007", "禁止访问"),
    INVALID_REQUEST("00008", "无效请求"),
    
    // ========== 用户相关错误码 (10000-19999) ==========
    USER_NOT_FOUND("10001", "用户不存在"),
    USER_DISABLED("10002", "用户已被禁用"),
    USER_ALREADY_EXISTS("10003", "用户已存在"),
    USERNAME_ALREADY_EXISTS("10004", "用户名已存在"),
    EMAIL_ALREADY_EXISTS("10005", "邮箱已存在"),
    PHONE_ALREADY_EXISTS("10006", "手机号已存在"),
    PASSWORD_ERROR("10007", "密码错误"),
    OLD_PASSWORD_ERROR("10008", "旧密码错误"),
    PASSWORD_TOO_SIMPLE("10009", "密码过于简单"),
    LOGIN_FAILED("10010", "登录失败"),
    TOKEN_EXPIRED("10011", "Token已过期"),
    TOKEN_INVALID("10012", "Token无效"),
    VERIFICATION_CODE_ERROR("10013", "验证码错误"),
    VERIFICATION_CODE_EXPIRED("10014", "验证码已过期"),
    
    // ========== 宠物相关错误码 (20000-29999) ==========
    PET_NOT_FOUND("20001", "宠物不存在"),
    PET_DISABLED("20002", "宠物已被删除"),
    PET_ALREADY_EXISTS("20003", "宠物已存在"),
    PET_LIMIT_EXCEEDED("20004", "宠物数量已达上限"),
    PET_TYPE_INVALID("20005", "宠物类型无效"),
    // ========== 宠物健康错误码 ==========
    HEALTH_RECORD_NOT_FOUND("21001", "健康记录不存在"),
    INVALID_RECORD_TYPE("21002", "无效的记录类型"),
    RECORD_VALUE_REQUIRED("21003", "记录数值不能为空"),
    
    // ========== 地址相关错误码 (30000-39999) ==========
    ADDRESS_NOT_FOUND("30001", "地址不存在"),
    ADDRESS_LIMIT_EXCEEDED("30002", "地址数量已达上限"),
    DEFAULT_ADDRESS_CANNOT_DELETE("30003", "默认地址不能删除"),
    
    // ========== 文件相关错误码 (40000-49999) ==========
    FILE_UPLOAD_FAILED("40001", "文件上传失败"),
    FILE_TOO_LARGE("40002", "文件过大"),
    FILE_TYPE_NOT_SUPPORTED("40003", "文件类型不支持"),
    FILE_NOT_FOUND("40004", "文件不存在"),
    
    // ========== 业务相关错误码 (50000-59999) ==========
    INSUFFICIENT_BALANCE("50001", "余额不足"),
    ORDER_NOT_FOUND("50002", "订单不存在"),
    ORDER_STATUS_ERROR("50003", "订单状态错误"),
    APPOINTMENT_CONFLICT("50004", "预约时间冲突"),
    SERVICE_UNAVAILABLE("50005", "服务不可用"),
    
    // ========== 第三方服务错误码 (60000-69999) ==========
    SMS_SEND_FAILED("60001", "短信发送失败"),
    EMAIL_SEND_FAILED("60002", "邮件发送失败"),
    PAYMENT_FAILED("60003", "支付失败"),
    THIRD_PARTY_SERVICE_ERROR("60004", "第三方服务异常");
    
    /**
     * 错误码
     */
    private final String code;
    
    /**
     * 错误信息
     */
    private final String message;
    
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    /**
     * 根据错误码获取枚举
     */
    public static ErrorCode getByCode(String code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return SYSTEM_ERROR;
    }
    
    /**
     * 判断是否为成功状态
     */
    public boolean isSuccess() {
        return SUCCESS.getCode().equals(this.code);
    }
    
    /**
     * 判断是否为系统错误
     */
    public boolean isSystemError() {
        return this.code.startsWith("0");
    }
    
    /**
     * 判断是否为业务错误
     */
    public boolean isBusinessError() {
        return !isSystemError() && !isSuccess();
    }
}