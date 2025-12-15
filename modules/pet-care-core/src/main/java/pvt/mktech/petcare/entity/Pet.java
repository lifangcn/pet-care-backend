package pvt.mktech.petcare.entity;

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
@Data
@Table("tb_pet")
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
     * 宠物类型: 1-狗 2-猫 3-其他
     */
    private Integer type;

    /**
     * 品种
     */
    private String breed;

    /**
     * 性别: 0-未知 1-雄性 2-雌性
     */
    private Boolean gender;

    /**
     * 生日
     */
    private Date birthday;

    /**
     * 体重(kg)
     */
    private BigDecimal weight;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 是否绝育: 0-否 1-是
     */
    private Boolean isSterilized;

    /**
     * 健康备注
     */
    private String healthNotes;

    /**
     * 过敏信息
     */
    private String allergyInfo;

    /**
     * 状态: 0-删除 1-正常
     */
    private Boolean status;

    /**
     * 创建时间
     */
    @Column(value = "created_at", onInsertValue = "CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(value = "updated_at", onUpdateValue = "CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;


}
