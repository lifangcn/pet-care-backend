package pvt.mktech.petcare.cdc.entity.core;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * {@code @description}: Label 表实体（仅查询用）
 * {@code @date}: 2026-01-27
 *
 * @author Michael
 */
@Data
@Table(value = "tb_label", dataSource = "core")
public class LabelEntity {

    @Id
    private Long id;

    private String name;
    private Integer type;
    private String icon;
    private String color;
    private Integer useCount;
    private Integer isRecommended;
    private Integer status;
}
