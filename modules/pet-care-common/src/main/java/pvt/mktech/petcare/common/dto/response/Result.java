package pvt.mktech.petcare.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * {@code @description}: 统一API响应结果封装类
 * {@code @param <T>} 数据载荷泛型类型
 */
@Data
public class Result<T> {
    /**
     * 请求处理结果状态标识
     * true: 请求成功
     * false: 请求失败
     */
    private String code;

    /**
     * 错误消息描述
     * 当 success 为 false 时有效
     */
    private String message;

    /**
     * 响应数据载荷
     * 当 success 为 true 时有效
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }


    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage("success");
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        return result;
    }

    public static <T> Result<T> error(ResultCode resultCode, String message) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(message);
        return result;
    }
}