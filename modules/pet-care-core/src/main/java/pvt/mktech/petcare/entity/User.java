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

    private String email;

    private String phone;

    private String nickname;

    private String avatarUrl;
    
    private Integer gender; // 0-未知 1-男 2-女
    
    private LocalDate birthday;
    
    private Integer status = 1; // 0-禁用 1-正常 2-未激活

//    private LocalDateTime lastLoginAt;

//    private String lastLoginIp;
    
    @Column(value = "created_at", onInsertValue = "CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
    
    @Column(value = "updated_at", onUpdateValue = "CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}