package pvt.mktech.petcare.core.util;

import com.mybatisflex.core.paginate.Page;
import pvt.mktech.petcare.common.dto.response.PageResult;

/**
 * {@code @description}:
 * {@code @date}: 2025/12/22 14:13
 *
 * @author Michael
 */
public class PageUtil {

    // 将 MyBatis-Flex 的分页结果转换为 PageResult
    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(
                page.getRecords(),
                page.getPageNumber(),
                page.getPageSize(),
                page.getTotalPage(),
                page.getTotalRow()
        );
    }
}
