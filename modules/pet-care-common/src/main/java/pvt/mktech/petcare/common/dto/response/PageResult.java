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
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {
    // 当前页记录
    private List<T> records;
    // 当前页码
    private Long pageNumber;
    // 页数
    private Long pageSize;
    // 总页数
    private Long totalPage;
    // 记录总行数
    private Long totalRow;
}