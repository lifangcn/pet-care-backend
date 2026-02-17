package pvt.mktech.petcare.social.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import pvt.mktech.petcare.social.entity.codelist.TypeOfInteraction;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 互动表（点赞/评分） 实体类。
 */
@Table("tb_interaction")
@Data
public class Interaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 动态ID
     */
    private Long postId;

    /**
     * 类型：LIKE-点赞 RATING-评分
     */
    private TypeOfInteraction interactionType;

    /**
     * 评分值 1-5
     */
    private Integer ratingValue;

    /**
     * 创建时间
     */
    @Column(value = "created_at", onInsertValue = "CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(value = "updated_at", onInsertValue = "CURRENT_TIMESTAMP", onUpdateValue = "CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}
