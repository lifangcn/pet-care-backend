package pvt.mktech.petcare.common.exception;

import lombok.Getter;

/**
 * 错误码枚举
 * 定义系统中所有的错误码和错误信息
 */
@Getter
public enum ErrorCode {

    // ========== 通用错误码 (00000-00999) ==========
    SUCCESS("00000", "操作成功"),
    SYSTEM_ERROR("00001", "系统异常，请稍后重试"),
    DATABASE_ERROR("00002", "数据库操作异常"),
    NETWORK_ERROR("00003", "网络异常"),
    LOCK_ACQUIRE_FAILED("00004", "获取分布式锁失败"),
    LOCK_INTERRUPTED("00005", "获取分布式锁被中断"),
    LOCK_OPERATION_FAILED("00006", "持有分布式锁的操作失败"),
    MESSAGE_SEND_FAILED("00007", "消息发送失败"),
    ID_GENERATOR_ERROR("00008", "ID生成器异常"),
    RATE_LIMIT_EXCEEDED("00009", "操作过于频繁，请稍后再试"),

    // ========== 参数校验错误码 (01000-01999) ==========
    PARAM_ERROR("01001", "参数错误"),
    PARAM_NULL("01002", "参数不能为空"),
    PARAM_INVALID("01003", "参数无效"),
    DATA_NOT_FOUND("01004", "数据不存在"),
    DATA_ALREADY_EXISTS("01005", "数据已存在"),
    OPERATION_FAILED("01006", "操作失败"),
    INVALID_REQUEST("01007", "无效请求"),
    
    // ========== 用户相关错误码 (10000-19999) ==========
    USER_NOT_FOUND("10001", "用户不存在"),
    USER_DISABLED("10002", "用户已被禁用"),
    USER_ALREADY_EXISTS("10003", "用户已存在"),
    USERNAME_ALREADY_EXISTS("10004", "用户名已存在"),
    EMAIL_ALREADY_EXISTS("10005", "邮箱已存在"),
    PHONE_ALREADY_EXISTS("10006", "手机号已存在"),
    PHONE_FORMAT_ERROR("10007", "手机号格式错误"),
    PASSWORD_ERROR("10008", "密码错误"),
    OLD_PASSWORD_ERROR("10009", "旧密码错误"),
    PASSWORD_TOO_SIMPLE("10010", "密码过于简单"),
    LOGIN_FAILED("10011", "登录失败"),
    TOKEN_EXPIRED("10012", "Token已过期"),
    TOKEN_INVALID("10013", "Token无效"),
    VERIFICATION_CODE_ERROR("10014", "验证码错误"),
    VERIFICATION_CODE_EXPIRED("10015", "验证码已过期"),
    UNAUTHORIZED("10016", "未授权访问"),
    FORBIDDEN("10017", "禁止访问"),

    USER_ALREADY_CHECKIN("10018", "用户已签到"),

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
    
    // ========== 文件相关错误码 (40000-49999) ==========
    FILE_NULL("40001", "文件不能为空"),
    FILE_TOO_LARGE("40002", "文件大小超出限制"),
    FILE_NAME_NULL("40003", "文件名不能为空"),
    FILE_URL_NULL("40004", "文件路径不能为空"),
    FILE_TYPE_NOT_SUPPORTED("40005", "文件类型不支持，仅支持：{0}"),
    FILE_TYPE_MISMATCH("40006", "文件内容与类型不匹配"),
    FILE_UPLOAD_FAILED("40007", "文件上传失败"),
    FILE_NOT_FOUND("40008", "文件不存在"),
    BUCKET_CREATE_FAILED("40009", "存储桶创建失败"),
    
    // ========== 业务相关错误码 (50000-59999) ==========
    INSUFFICIENT_BALANCE("50001", "余额不足"),
    ORDER_NOT_FOUND("50002", "订单不存在"),
    ORDER_STATUS_ERROR("50003", "订单状态错误"),
    APPOINTMENT_CONFLICT("50004", "预约时间冲突"),
    SERVICE_UNAVAILABLE("50005", "服务不可用"),
    POINTS_NOT_ENOUGH("50006", "积分不足"),
    POINTS_ACCOUNT_NOT_FOUND("50007", "积分账户不存在"),
    
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