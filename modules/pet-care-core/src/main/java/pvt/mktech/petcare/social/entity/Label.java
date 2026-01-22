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
 * 标签表 实体类。
 */
@Table("tb_label")
@Data
public class Label implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 标签名
     */
    private String name;

    /**
     * 1-通用标签 2-宠物品种 3-内容类型
     */
    private Integer type;

    /**
     * 标签图标
     */
    private String icon;

    /**
     * 标签颜色
     */
    private String color;

    /**
     * 使用次数
     */
    private Integer useCount;

    /**
     * 是否推荐标签 0-否 1-是
     */
    private Integer isRecommended;

    /**
     * 状态：1-正常，0-禁用
     */
    private Integer status = 1;

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

    /**
     * 逻辑删除：0-正常，1-已删除
     */
    @Column(value = "is_deleted", onInsertValue = "0")
    private Integer isDeleted;

    /**
     * 删除时间
     */
    @Column(value = "deleted_at")
    private LocalDateTime deletedAt;
}
