package pvt.mktech.petcare.cdc.entity.core;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/**
 * {@code @description}: PostLabel 表实体（仅查询用）
 * {@code @date}: 2026-01-27
 *
 * @author Michael
 */
@Data
@Table(value = "tb_post_label", dataSource = "core")
public class PostLabelEntity {

    @Id
    private Long id;

    private Long postId;
    private Long labelId;
}
