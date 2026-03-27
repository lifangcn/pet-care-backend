package pvt.mktech.petcare.user.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色实体类
 * 对应数据库表 tb_role
 */
@Data
@Table("tb_role")
public class Role implements Serializable {

    @Serial
    private static final long serialVersionUID = -1234567890123456789L;

    /**
     * 角色ID，主键，自增
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 角色编码（如：admin, user）
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sort = 0;

    /**
     * 状态：1-启用，0-禁用
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
    @Column(value = "updated_at", onInsertValue = "CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除：0-正常，1-已删除
     */
    @Column(value = "is_deleted", onInsertValue = "0")
    private Boolean isDeleted;

    /**
     * 删除时间
     */
    @Column(value = "deleted_at")
    private LocalDateTime deletedAt;
}