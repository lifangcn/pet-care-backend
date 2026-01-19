package pvt.mktech.petcare.club.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.club.entity.Label;
import pvt.mktech.petcare.club.mapper.LabelMapper;
import pvt.mktech.petcare.club.service.LabelService;

import java.util.List;

import static pvt.mktech.petcare.club.entity.table.LabelTableDef.LABEL;

/**
 * 标签表 服务层实现。
 */
@Service
public class LabelServiceImpl extends ServiceImpl<LabelMapper, Label> implements LabelService {

    @Override
    public List<Label> getLabelList(Integer type) {
        QueryWrapper queryWrapper = QueryWrapper.create().orderBy(LABEL.USE_COUNT.desc());
        if (type != null) {
            queryWrapper.where(LABEL.TYPE.eq(type));
        }
        return list(queryWrapper);
    }

    @Override
    public List<Label> getHotLabels() {
        return list(QueryWrapper.create()
                .where(LABEL.IS_RECOMMENDED.eq(1))
                .orderBy(LABEL.USE_COUNT.desc())
                .limit(20));
    }

    @Override
    public List<Label> suggestLabels(String keyword) {
        return list(QueryWrapper.create()
                .where(LABEL.NAME.like("%" + keyword + "%"))
                .orderBy(LABEL.USE_COUNT.desc())
                .limit(10));
    }

    @Override
    public void incrementUseCount(Long labelId) {
        Label label = getById(labelId);
        if (label != null) {
            label.setUseCount((label.getUseCount() == null ? 0 : label.getUseCount()) + 1);
            updateById(label);
        }
    }
}
