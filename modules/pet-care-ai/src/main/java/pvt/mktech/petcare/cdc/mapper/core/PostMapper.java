package pvt.mktech.petcare.cdc.mapper.core;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import pvt.mktech.petcare.cdc.entity.core.PostEntity;

/**
 * {@code @description}: Post Mapper（仅查询，使用 core 数据源）
 * {@code @date}: 2026-01-27
 * @author Michael
 */
@Mapper
@UseDataSource("core")
public interface PostMapper extends BaseMapper<PostEntity> {
}
