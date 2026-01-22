package pvt.mktech.petcare.social.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 动态标签关联表 实体类。
 */
@Table("tb_post_label")
@Data
public class PostLabel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 动态ID
     */
    private Long postId;

    /**
     * 标签ID
     */
    private Long labelId;

    /**
     * 创建时间
     */
    @Column(value = "created_at", onInsertValue = "CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
