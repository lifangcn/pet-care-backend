package pvt.mktech.petcare.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("tb_user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = -7194183884668832164L;

    @Id(keyType = KeyType.Auto)
    private Long id;
    
    private String username;

    private String phone;

    private String nickname;

    private String avatar;

    private Integer status = 1;

    private String address;
    
    @Column(value = "created_at", onInsertValue = "CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
    
    @Column(value = "updated_at", onUpdateValue = "CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}