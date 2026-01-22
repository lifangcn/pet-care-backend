package pvt.mktech.petcare.health.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import pvt.mktech.petcare.health.dto.request.HealthRecordQueryRequest;
import pvt.mktech.petcare.health.entity.HealthRecord;

/**
 * 健康记录表 服务层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-22
 */
public interface HealthRecordService extends IService<HealthRecord> {

    Page<HealthRecord> findPageByQueryRequest(Long pageNumber, Long pageSize, HealthRecordQueryRequest request);
}
