package pvt.mktech.petcare.sync.dto.event;

/**
 * {@code @description}: CDC 数据基接口（标记接口）
 * <p>所有CDC数据DTO都应实现此接口</p>
 * {@code @date}: 2026-01-30
 * @author Michael
 */
public interface CdcData {
    /**
     * 获取实体ID
     */
    Long getId();
}
