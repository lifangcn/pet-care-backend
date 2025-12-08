package pvt.mktech.petcare.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import pvt.mktech.petcare.entity.User;

import java.util.Optional;


public interface UserMapper extends BaseMapper<User> {
}