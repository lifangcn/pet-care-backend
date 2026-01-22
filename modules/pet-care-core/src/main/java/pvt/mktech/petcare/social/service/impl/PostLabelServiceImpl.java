package pvt.mktech.petcare.social.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.social.entity.Label;
import pvt.mktech.petcare.social.entity.PostLabel;
import pvt.mktech.petcare.social.mapper.PostLabelMapper;
import pvt.mktech.petcare.social.service.LabelService;
import pvt.mktech.petcare.social.service.PostLabelService;

import java.util.ArrayList;
import java.util.List;

import static pvt.mktech.petcare.social.entity.table.LabelTableDef.LABEL;
import static pvt.mktech.petcare.social.entity.table.PostLabelTableDef.POST_LABEL;

/**
 * {@code @description}: 动态标签关联 服务层实现
 * {@code @date}: 2025-01-22
 * {@code @author}: Michael
 */
@Service
@RequiredArgsConstructor
public class PostLabelServiceImpl extends ServiceImpl<PostLabelMapper, PostLabel> implements PostLabelService {

    private final LabelService labelService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePostLabels(Long postId, List<Long> labelIds) {
        if (labelIds == null || labelIds.isEmpty()) {
            return;
        }
        List<PostLabel> postLabels = new ArrayList<>();
        for (Long labelId : labelIds) {
            PostLabel postLabel = new PostLabel();
            postLabel.setPostId(postId);
            postLabel.setLabelId(labelId);
            postLabels.add(postLabel);
            // 增加标签使用次数
            labelService.incrementUseCount(labelId);
        }
        saveBatch(postLabels);
    }

    @Override
    public List<Label> listLabelsByPostId(Long postId) {
        // 通过 PostLabel 关联查询 Label
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(LABEL.ALL_COLUMNS)
                .from(POST_LABEL)
                .leftJoin(LABEL).on(POST_LABEL.LABEL_ID.eq(LABEL.ID))
                .where(POST_LABEL.POST_ID.eq(postId));
        return labelService.list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByPostId(Long postId) {
        remove(QueryWrapper.create().where(POST_LABEL.POST_ID.eq(postId)));
    }
}
