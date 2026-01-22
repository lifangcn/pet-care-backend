package pvt.mktech.petcare.social.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.social.dto.request.ActivityQueryRequest;
import pvt.mktech.petcare.social.entity.Activity;
import pvt.mktech.petcare.social.entity.Post;
import pvt.mktech.petcare.social.mapper.ActivityMapper;
import pvt.mktech.petcare.social.mapper.PostMapper;
import pvt.mktech.petcare.social.service.ActivityService;
import pvt.mktech.petcare.social.service.PostService;
import pvt.mktech.petcare.common.exception.BusinessException;

import java.util.List;

import static pvt.mktech.petcare.social.entity.table.ActivityTableDef.ACTIVITY;
import static pvt.mktech.petcare.social.entity.table.PostTableDef.POST;

/**
 * 活动表 服务层实现。
 */
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    private final PostService postService;

    @Override
    public Activity createActivity(Activity activity) {
        save(activity);
        return activity;
    }

    @Override
    public Page<Activity> getActivityList(Long pageNumber, Long pageSize, ActivityQueryRequest request) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .orderBy(ACTIVITY.ACTIVITY_TIME.desc());

        if (request.getStatus() != null) {
            queryWrapper.and(ACTIVITY.STATUS.eq(request.getStatus()));
        }
        if (request.getActivityType() != null) {
            queryWrapper.and(ACTIVITY.ACTIVITY_TYPE.eq(request.getActivityType()));
        }

        return page(Page.of(pageNumber, pageSize), queryWrapper);
    }

    @Override
    public Activity getActivityDetail(Long id) {
        return getById(id);
    }

    @Override
    public Boolean joinActivity(Long userId, Long activityId) {
        Activity activity = getById(activityId);
        if (activity == null) {
            throw new BusinessException("404", "活动不存在");
        }
        if (activity.getStatus() != 1) {
            throw new BusinessException("400", "活动不在招募中");
        }
        if (activity.getMaxParticipants() > 0 && activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
            throw new BusinessException("400", "活动人数已满");
        }
        // 是否已经报名
        Boolean isJoined = postService.hasJoinActivity(userId, activityId);

        if (isJoined) {
            throw new BusinessException("400", "您已经报名");
        }

        // 生成报名动态 postType = 6
        Post post = new Post();
        post.setUserId(userId);
        post.setPostType(6);
        post.setActivityId(activityId);
        post.setStatus(1);
        postService.save(post);

        activity.setCurrentParticipants((activity.getCurrentParticipants() == null ? 0 : activity.getCurrentParticipants()) + 1);
        return updateById(activity);
    }

    @Override
    public Post checkInActivity(Long userId, Long activityId, Post post) {
        Activity activity = getById(activityId);
        if (activity == null) {
            throw new BusinessException("404", "活动不存在");
        }
        if (activity.getCheckInEnabled() == null || activity.getCheckInEnabled() != 1) {
            throw new BusinessException("400", "活动未开启打卡");
        }
        // 创建打卡动态
        post.setUserId(userId);
        post.setPostType(5);
        post.setActivityId(activityId);
        post.setStatus(1);
        postService.save(post);

        activity.setCheckInCount((activity.getCheckInCount() == null ? 0 : activity.getCheckInCount()) + 1);
        updateById(activity);

        return post;
    }
}
