package pvt.mktech.petcare.social.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.social.dto.request.PostQueryRequest;
import pvt.mktech.petcare.social.dto.request.PostSaveRequest;
import pvt.mktech.petcare.social.dto.response.PostDetailResponse;
import pvt.mktech.petcare.social.entity.Interaction;
import pvt.mktech.petcare.social.entity.Post;
import pvt.mktech.petcare.social.mapper.PostMapper;
import pvt.mktech.petcare.social.service.InteractionService;
import pvt.mktech.petcare.social.service.LabelService;
import pvt.mktech.petcare.social.service.PostLabelService;
import pvt.mktech.petcare.social.service.PostService;
import pvt.mktech.petcare.common.usercache.UserContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static pvt.mktech.petcare.social.entity.table.InteractionTableDef.INTERACTION;
import static pvt.mktech.petcare.social.entity.table.PostTableDef.POST;

/**
 * 动态表 服务层实现。
 */
@Service
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private final InteractionService interactionService;
    private final LabelService labelService;
    private final PostLabelService postLabelService;

    @Override
    public Post savePost(PostSaveRequest request) {
        Post post = BeanUtil.copyProperties(request, Post.class);
        post.setUserId(UserContext.getUserId());
        save(post);
        // 处理标签关联
        if (request.getLabelIds() != null && !request.getLabelIds().isEmpty()) {
            postLabelService.savePostLabels(post.getId(), request.getLabelIds());
        }
        return post;
    }

    @Override
    public Page<Post> findPageByQueryRequest(Long pageNumber, Long pageSize, PostQueryRequest request) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(POST.STATUS.eq(1))
                .orderBy(POST.CREATED_AT.desc());

        if (request.getPostType() != null) {
            queryWrapper.and(POST.POST_TYPE.eq(request.getPostType()));
        } else {
            // 如果查询动态类型为空，默认为内容广场查询 1～4的分享内容
            queryWrapper.and(POST.POST_TYPE.in(1, 2, 3, 4));
        }

        // 城市筛选
        if (StrUtil.isNotBlank(request.getCity())) {
            queryWrapper.and(POST.LOCATION_INFO.like("%" + request.getCity() + "%"));
        }

        // 排序
        if ("hottest".equals(request.getSortBy())) {
            queryWrapper.orderBy(POST.VIEW_COUNT.desc(), POST.LIKE_COUNT.desc());
        } else if ("rating".equals(request.getSortBy())) {
            queryWrapper.orderBy(POST.RATING_AVG.desc());
        }

        return page(Page.of(pageNumber, pageSize), queryWrapper);
    }

    @Override
    public PostDetailResponse getPostDetail(Long id) {
        PostDetailResponse response = getOneAs(QueryWrapper.create()
                .select(POST.ALL_COLUMNS)
                .from(POST)
                .where(POST.ID.eq(id)), PostDetailResponse.class);
        if (response == null) {
            return null;
        }
        incrementViewCount(id);
        // 查询我的评分
        Long userId = UserContext.getUserId();
        Integer userRatingValue = interactionService.getUserRating(userId, id);
        response.setUserRatingValue(userRatingValue);
        // 查询标签
        response.setLabels(postLabelService.listLabelsByPostId(id));
        return response;
    }

    @Override
    public boolean updatePost(Long id, Post post) {
        post.setId(id);
        return updateById(post);
    }

    @Override
    public boolean deletePost(Long id) {
        Post post = new Post();
        post.setId(id);
        post.setStatus(3);
        return updateById(post);
    }

    @Override
    public void incrementViewCount(Long id) {
        // TODO 扩展点：使用 Redis 计数，定时同步到数据库
        // 这里简单实现，直接更新数据库
        Post post = getById(id);
        if (post != null) {
            post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
            updateById(post);
        }
    }

    @Override
    public List<Post> listParticipantsByActivityId(Long activityId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(POST.ACTIVITY_ID.eq(activityId))
                .and(POST.POST_TYPE.eq(6))
                .and(POST.STATUS.eq(1))
                .orderBy(POST.CREATED_AT.desc());
        return list(queryWrapper);
    }

    @Override
    public Boolean hasJoinActivity(Long userId, Long activityId) {
        return exists(QueryWrapper.create()
                .where(POST.USER_ID.eq(userId))
                .and(POST.ACTIVITY_ID.eq(activityId))
                .and(POST.POST_TYPE.eq(6))
        );
    }

    @Override
    public boolean toggleLike(Long postId) {
        Post post = getById(postId);
        if (post == null) {
            throw new IllegalArgumentException("动态不存在");
        }
        Long userId = UserContext.getUserId();
        boolean isLiked = interactionService.toggleLike(userId, postId);
        updateLikeCount(postId, isLiked ? 1 : -1);
        return isLiked;
    }

    @Override
    public boolean rate(Long postId, Integer rating) {
        Post post = getById(postId);
        if (post == null) {
            throw new IllegalArgumentException("动态不存在");
        }
        Long userId = UserContext.getUserId();
        boolean success = interactionService.rate(userId, postId, rating);
        if (success) {
            updateRatingStats(postId);
        }
        return success;
    }

    private void updateLikeCount(Long postId, int delta) {
        Post post = getById(postId);
        if (post != null) {
            int newCount = (post.getLikeCount() == null ? 0 : post.getLikeCount()) + delta;
            post.setLikeCount(Math.max(newCount, 0));
            updateById(post);
        }
    }

    private void updateRatingStats(Long postId) {
        List<Interaction> ratings = interactionService.list(QueryWrapper.create()
                .select(INTERACTION.RATING_VALUE)
                .where(INTERACTION.POST_ID.eq(postId))
                .and(INTERACTION.INTERACTION_TYPE.eq(2)));

        if (!ratings.isEmpty()) {
            int sum = ratings.stream().mapToInt(r -> r.getRatingValue() == null ? 0 : r.getRatingValue()).sum();
            BigDecimal avg = BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
            Post post = getById(postId);
            if (post != null) {
                post.setRatingCount(ratings.size());
                post.setRatingTotal(sum);
                post.setRatingAvg(avg);
                updateById(post);
            }
        }
    }
}
