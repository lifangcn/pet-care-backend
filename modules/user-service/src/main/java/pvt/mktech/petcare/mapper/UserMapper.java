package pvt.mktech.petcare.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import pvt.mktech.petcare.entity.User;

import java.util.Optional;


public interface UserMapper extends BaseMapper<User> {
    
    @Select("SELECT * FROM pc_users WHERE username = #{username} AND status = 1")
    Optional<User> findByUsername(@Param("username") String username);
    
    @Select("SELECT * FROM pc_users WHERE email = #{email} AND status = 1")
    Optional<User> findByEmail(@Param("email") String email);
    
    @Select("SELECT * FROM pc_users WHERE phone = #{phone} AND status = 1")
    Optional<User> findByPhone(@Param("phone") String phone);
    
    @Select("SELECT * FROM pc_users WHERE (username = #{identifier} OR email = #{identifier} OR phone = #{identifier}) AND status = 1")
    Optional<User> findByIdentifier(@Param("identifier") String identifier);
    
    @Select("SELECT COUNT(*) FROM pc_users WHERE username = #{username}")
    boolean existsByUsername(@Param("username") String username);
    
    @Select("SELECT COUNT(*) FROM pc_users WHERE email = #{email}")
    boolean existsByEmail(@Param("email") String email);
    
    @Select("SELECT COUNT(*) FROM pc_users WHERE phone = #{phone}")
    boolean existsByPhone(@Param("phone") String phone);
}