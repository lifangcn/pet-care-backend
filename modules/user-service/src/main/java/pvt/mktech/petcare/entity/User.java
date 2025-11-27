package pvt.mktech.petcare.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("tb_users")
public class User {
    
    @Id(keyType = KeyType.Auto)
    private Long id;
    
    @Column("username")
    private String username;
    
    @Column("email")
    private String email;
    
    @Column("phone")
    private String phone;
    
    @Column("password_hash")
    private String passwordHash;
    
    @Column("nickname")
    private String nickname;
    
    @Column("avatar_url")
    private String avatarUrl;
    
    @Column("gender")
    private Integer gender; // 0-未知 1-男 2-女
    
    @Column("birthday")
    private LocalDate birthday;
    
    @Column("status")
    private Integer status = 1; // 0-禁用 1-正常 2-未激活


    @Column("last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column("last_login_ip")
    private String lastLoginIp;
    
    @Column(value = "created_at", onInsertValue = "CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
    
    @Column(value = "updated_at", onUpdateValue = "CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}