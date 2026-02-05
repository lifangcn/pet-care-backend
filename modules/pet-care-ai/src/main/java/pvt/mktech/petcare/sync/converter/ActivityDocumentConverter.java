package pvt.mktech.petcare.sync.converter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.sync.dto.EsActivityDocument;
import pvt.mktech.petcare.sync.dto.event.ActivityCdcData;
import pvt.mktech.petcare.sync.util.DateTimeConverter;

import java.time.Instant;

/**
 * {@code @description}: Activity文档转换器
 * <p>将ActivityCdcData转换为EsActivityDocument</p>
 * {@code @date}: 2026-01-30
 * @author Michael
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityDocumentConverter implements DocumentConverter<ActivityCdcData, EsActivityDocument> {

    @Override
    public EsActivityDocument convert(ActivityCdcData cdcData) {
        EsActivityDocument doc = new EsActivityDocument();
        doc.setId(cdcData.getId());
        doc.setUserId(cdcData.getUserId());
        doc.setTitle(cdcData.getTitle());
        doc.setDescription(cdcData.getDescription());
        doc.setActivityType(cdcData.getActivityType());
        doc.setActivityTime(Instant.ofEpochMilli(cdcData.getActivityTime()));
        doc.setEndTime(Instant.ofEpochMilli(cdcData.getEndTime()));
        doc.setAddress(cdcData.getAddress());
        doc.setOnlineLink(cdcData.getOnlineLink());
        doc.setMaxParticipants(cdcData.getMaxParticipants());
        doc.setCurrentParticipants(cdcData.getCurrentParticipants());
        doc.setStatus(cdcData.getStatus());
        doc.setCheckInEnabled(cdcData.getCheckInEnabled());
        doc.setCheckInCount(cdcData.getCheckInCount());
        doc.setCreatedAt(Instant.ofEpochMilli(cdcData.getCreatedAt()));
        return doc;
    }
}
