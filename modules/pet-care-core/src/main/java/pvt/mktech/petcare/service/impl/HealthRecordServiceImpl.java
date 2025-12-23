package pvt.mktech.petcare.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.dto.request.HealthRecordQueryRequest;
import pvt.mktech.petcare.entity.HealthRecord;
import pvt.mktech.petcare.mapper.HealthRecordMapper;
import pvt.mktech.petcare.service.HealthRecordService;

import static pvt.mktech.petcare.entity.table.HealthRecordTableDef.HEALTH_RECORD;

/**
 * 健康记录表 服务层实现。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HealthRecordServiceImpl extends ServiceImpl<HealthRecordMapper, HealthRecord> implements HealthRecordService {

    private final HealthRecordMapper healthRecordMapper;

    @Override
    public Page<HealthRecord> findPageByQueryRequest(HealthRecordQueryRequest request) {
        QueryWrapper queryWrapper = queryChain()
                .select(HEALTH_RECORD.ALL_COLUMNS)
                .where(HEALTH_RECORD.PET_ID.eq(request.getPetId()));
        if (StrUtil.isNotEmpty(request.getRecordType())) {
            queryWrapper.and(HEALTH_RECORD.RECORD_TYPE.eq(request.getRecordType()));
        }
        return page(Page.of(request.getPageNumber(), request.getPageSize()), queryWrapper);
    }
}
