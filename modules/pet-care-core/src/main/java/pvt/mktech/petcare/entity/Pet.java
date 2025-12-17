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

    private Long userId;

    private String name;

    private Integer type;

    private String breed;

    private Boolean gender;

    private Date birthday;

    private BigDecimal weight;

    private String avatar;

    private String healthNotes;

    @Column(value = "created_at", onInsertValue = "CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
