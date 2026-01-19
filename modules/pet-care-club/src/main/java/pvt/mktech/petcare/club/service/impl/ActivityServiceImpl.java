package pvt.mktech.petcare.club.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.club.dto.request.ActivityQueryRequest;
import pvt.mktech.petcare.club.entity.Activity;
import pvt.mktech.petcare.club.entity.Post;
import pvt.mktech.petcare.club.mapper.ActivityMapper;
import pvt.mktech.petcare.club.mapper.PostMapper;
import pvt.mktech.petcare.club.service.ActivityService;
import pvt.mktech.petcare.common.exception.BusinessException;

import java.util.List;

import static pvt.mktech.petcare.club.entity.table.ActivityTableDef.ACTIVITY;
import static pvt.mktech.petcare.club.entity.table.PostTableDef.POST;

/**
 * 活动表 服务层实现。
 */
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    private final PostMapper postMapper;

    @Override
    public Activity createActivity(Activity activity) {
        save(activity);
        return activity;
    }

    @Override
    public Page<Activity> getActivityList(ActivityQueryRequest request) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .orderBy(ACTIVITY.ACTIVITY_TIME.desc());

        if (request.getStatus() != null) {
            queryWrapper.and(ACTIVITY.STATUS.eq(request.getStatus()));
        }
        if (request.getActivityType() != null) {
            queryWrapper.and(ACTIVITY.ACTIVITY_TYPE.eq(request.getActivityType()));
        }

        return page(Page.of(request.getPageNumber(), request.getPageSize()), queryWrapper);
    }

    @Override
    public Activity getActivityDetail(Long id) {
        return getById(id);
    }

    @Override
    public boolean joinActivity(Long userId, Long activityId) {
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

        activity.setCurrentParticipants((activity.getCurrentParticipants() == null ? 0 : activity.getCurrentParticipants()) + 1);
        return updateById(activity);
    }

    @Override
    public Post checkinActivity(Long userId, Long activityId, Post post) {
        Activity activity = getById(activityId);
        if (activity == null) {
            throw new BusinessException("404", "活动不存在");
        }
        if (activity.getCheckinEnabled() == null || activity.getCheckinEnabled() != 1) {
            throw new BusinessException("400", "活动未开启打卡");
        }

        post.setUserId(userId);
        post.setPostType(5);
        post.setActivityId(activityId);
        post.setIsCheckin(1);
        postMapper.insert(post);

        activity.setCheckinCount((activity.getCheckinCount() == null ? 0 : activity.getCheckinCount()) + 1);
        updateById(activity);

        return post;
    }

    @Override
    public List<Post> getActivityCheckins(Long activityId) {
        return postMapper.selectListByQuery(QueryWrapper.create()
                .where(POST.ACTIVITY_ID.eq(activityId))
                .and(POST.IS_CHECKIN.eq(1))
                .orderBy(POST.CREATED_AT.desc()));
    }
}
