package pvt.mktech.petcare.core.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表 tb_user
 */
@Data
@Table("tb_user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = -7194183884668832164L;
    /**
     * 用户ID，主键，自增
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 用户状态：1-正常，0-禁用
     */
    private Integer status = 1;

    /**
     * 地址信息
     */
    private String address;

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
}