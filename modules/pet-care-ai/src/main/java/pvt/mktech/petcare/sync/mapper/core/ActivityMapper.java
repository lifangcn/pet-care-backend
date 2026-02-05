package pvt.mktech.petcare.sync.mapper.core;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import pvt.mktech.petcare.sync.entity.core.ActivityEntity;

/**
 * {@code @description}: Activity Mapper（仅查询，使用 core 数据源）
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Mapper
@UseDataSource("core")
public interface ActivityMapper extends BaseMapper<ActivityEntity> {
}
