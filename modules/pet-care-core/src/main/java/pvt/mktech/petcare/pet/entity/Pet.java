package pvt.mktech.petcare.pet.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;


/**
 * 宠物表 实体类。
 *
 * @author PetCare Code Generator
 * @since 2025-12-02
 */
@Table("tb_pet")
@Data
public class Pet implements Serializable {

    @Serial
    private static final long serialVersionUID = -8556286714851982180L;
    /**
     * 宠物ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 宠物名称
     */
    private String name;

    /**
     * 宠物类型
     */
    private String type;

    /**
     * 种类
     */
    private String breed;

    /**
     * 性别：MALE-公 FEMALE-母
     */
    private String gender;

    /**
     * 出生日期
     */
    private Date birthday;

    /**
     * 体重
     */
    private BigDecimal weight;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 健康备注
     */
    private String healthNotes;

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
