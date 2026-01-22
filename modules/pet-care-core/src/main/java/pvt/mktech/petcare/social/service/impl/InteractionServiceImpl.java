package pvt.mktech.petcare.social.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.social.entity.Interaction;
import pvt.mktech.petcare.social.mapper.InteractionMapper;
import pvt.mktech.petcare.social.service.InteractionService;

import static pvt.mktech.petcare.social.entity.table.InteractionTableDef.INTERACTION;

/**
 * {@code @description}: 互动表 服务层实现
 * {@code @date}: 2025-01-21
 * {@code @author}: Michael
 */
@Service
@RequiredArgsConstructor
public class InteractionServiceImpl extends ServiceImpl<InteractionMapper, Interaction> implements InteractionService {

    @Override
    public boolean toggleLike(Long userId, Long postId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(INTERACTION.USER_ID.eq(userId))
                .and(INTERACTION.POST_ID.eq(postId))
                .and(INTERACTION.INTERACTION_TYPE.eq(1));
        Interaction exist = getOne(queryWrapper);

        if (exist != null) {
            removeById(exist.getId());
            return false;
        } else {
            Interaction interaction = new Interaction();
            interaction.setUserId(userId);
            interaction.setPostId(postId);
            interaction.setInteractionType(1);
            save(interaction);
            return true;
        }
    }

    @Override
    public boolean rate(Long userId, Long postId, Integer rating) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(INTERACTION.USER_ID.eq(userId))
                .and(INTERACTION.POST_ID.eq(postId))
                .and(INTERACTION.INTERACTION_TYPE.eq(2));
        Interaction exist = getOne(queryWrapper);

        if (exist != null) {
            exist.setRatingValue(rating);
            updateById(exist);
        } else {
            Interaction interaction = new Interaction();
            interaction.setUserId(userId);
            interaction.setPostId(postId);
            interaction.setInteractionType(2);
            interaction.setRatingValue(rating);
            save(interaction);
        }
        return true;
    }

    @Override
    public Integer getUserRating(Long userId, Long id) {
        Interaction interaction = getOne(QueryWrapper.create()
                .select(INTERACTION.RATING_VALUE)
                .where(INTERACTION.USER_ID.eq(userId))
                .and(INTERACTION.POST_ID.eq(id))
                .and(INTERACTION.INTERACTION_TYPE.eq(2)));
        return interaction != null ? interaction.getRatingValue() : null;
    }
}
