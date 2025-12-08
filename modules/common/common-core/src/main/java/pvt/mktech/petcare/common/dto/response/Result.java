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
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    /**
     * 请求处理结果状态标识
     * true: 请求成功
     * false: 请求失败
     */
    private Boolean success;

    /**
     * 错误消息描述
     * 当 success 为 false 时有效
     */
    private String errorMsg;

    /**
     * 响应数据载荷
     * 当 success 为 true 时有效
     */
    private T data;

    /**
     * 数据总量（用于分页场景）
     */
    private Long total;

    /**
     * 创建成功的无数据响应
     *
     * @param <T> 数据类型
     * @return 成功响应实例
     */
    public static <T> Result<T> ok(){
        return new Result<>(true, null, null, null);
    }

    /**
     * 创建带数据的成功响应
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应实例
     */
    public static <T> Result<T> ok(T data){
        return new Result<>(true, null, data, null);
    }

    /**
     * 创建带分页数据的成功响应
     *
     * @param data 数据列表
     * @param total 数据总量
     * @param <T> 数据类型
     * @return 成功响应实例
     */
    public static <T> Result<List<T>> ok(List<T> data, Long total){
        return new Result<>(true, null, data, total);
    }

    /**
     * 创建失败响应
     *
     * @param errorMsg 错误信息
     * @param <T> 数据类型
     * @return 失败响应实例
     */
    public static <T> Result<T> fail(String errorMsg){
        return new Result<>(false, errorMsg, null, null);
    }
}