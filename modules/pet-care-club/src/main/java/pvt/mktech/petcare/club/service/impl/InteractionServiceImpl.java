package pvt.mktech.petcare.club.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.club.entity.Interaction;
import pvt.mktech.petcare.club.entity.Post;
import pvt.mktech.petcare.club.mapper.InteractionMapper;
import pvt.mktech.petcare.club.mapper.PostMapper;
import pvt.mktech.petcare.club.service.InteractionService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static pvt.mktech.petcare.club.entity.table.InteractionTableDef.INTERACTION;
import static pvt.mktech.petcare.club.entity.table.PostTableDef.POST;

/**
 * 互动表 服务层实现。
 */
@Service
@RequiredArgsConstructor
public class InteractionServiceImpl extends ServiceImpl<InteractionMapper, Interaction> implements InteractionService {

    private final PostMapper postMapper;

    @Override
    public boolean toggleLike(Long userId, Long postId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(INTERACTION.USER_ID.eq(userId))
                .and(INTERACTION.POST_ID.eq(postId))
                .and(INTERACTION.INTERACTION_TYPE.eq(1));
        Interaction exist = getOne(queryWrapper);

        if (exist != null) {
            removeById(exist.getId());
            updateLikeCount(postId, -1);
            return false;
        } else {
            Interaction interaction = new Interaction();
            interaction.setUserId(userId);
            interaction.setPostId(postId);
            interaction.setInteractionType(1);
            save(interaction);
            updateLikeCount(postId, 1);
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
        updateRatingStats(postId);
        return true;
    }

    @Override
    public BigDecimal getRatingAvg(Long postId) {
        Post post = postMapper.selectOneById(postId);
        return post != null ? post.getRatingAvg() : BigDecimal.ZERO;
    }

    private void updateLikeCount(Long postId, int delta) {
        Post post = postMapper.selectOneById(postId);
        if (post != null) {
            int newCount = (post.getLikeCount() == null ? 0 : post.getLikeCount()) + delta;
            post.setLikeCount(Math.max(newCount, 0));
            postMapper.update(post);
        }
    }

    private void updateRatingStats(Long postId) {
        List<Interaction> ratings = list(QueryWrapper.create()
                .where(INTERACTION.POST_ID.eq(postId))
                .and(INTERACTION.INTERACTION_TYPE.eq(2)));

        if (!ratings.isEmpty()) {
            int sum = ratings.stream().mapToInt(r -> r.getRatingValue() == null ? 0 : r.getRatingValue()).sum();
            BigDecimal avg = BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
            Post post = postMapper.selectOneById(postId);
            if (post != null) {
                post.setRatingCount(ratings.size());
                post.setRatingTotal(sum);
                post.setRatingAvg(avg);
                postMapper.update(post);
            }
        }
    }
}
