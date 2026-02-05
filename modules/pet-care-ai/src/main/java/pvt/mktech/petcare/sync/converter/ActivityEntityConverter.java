package pvt.mktech.petcare.sync.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.sync.dto.EsActivityDocument;
import pvt.mktech.petcare.sync.entity.core.ActivityEntity;
import pvt.mktech.petcare.sync.util.DateTimeConverter;

/**
 * {@code @description}: Activity实体转换器
 * <p>将ActivityEntity转换为EsActivityDocument（用于数据迁移）</p>
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEntityConverter {

    /**
     * 将ActivityEntity转换为EsActivityDocument
     */
    public EsActivityDocument convert(ActivityEntity entity) {
        EsActivityDocument doc = new EsActivityDocument();
        doc.setId(entity.getId());
        doc.setUserId(entity.getUserId());
        doc.setTitle(entity.getTitle());
        doc.setDescription(entity.getDescription());
        doc.setActivityType(entity.getActivityType());
        doc.setActivityTime(DateTimeConverter.toInstant(entity.getActivityTime()));
        doc.setEndTime(DateTimeConverter.toInstant(entity.getEndTime()));
        doc.setAddress(entity.getAddress());
        doc.setOnlineLink(entity.getOnlineLink());
        doc.setMaxParticipants(entity.getMaxParticipants());
        doc.setCurrentParticipants(entity.getCurrentParticipants());
        doc.setStatus(entity.getStatus());
        doc.setCheckInEnabled(entity.getCheckInEnabled());
        doc.setCheckInCount(entity.getCheckInCount());
        doc.setCreatedAt(DateTimeConverter.toInstant(entity.getCreatedAt()));
        return doc;
    }
}
