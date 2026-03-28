package pvt.mktech.petcare.social.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.social.dto.request.ActivityQueryRequest;
import pvt.mktech.petcare.social.entity.Activity;
import pvt.mktech.petcare.social.entity.Post;
import pvt.mktech.petcare.social.entity.codelist.AuditStatusOfContent;
import pvt.mktech.petcare.social.entity.codelist.StatusOfActivity;
import pvt.mktech.petcare.social.entity.codelist.TypeOfPost;
import pvt.mktech.petcare.social.mapper.ActivityMapper;
import pvt.mktech.petcare.social.service.ActivityService;
import pvt.mktech.petcare.social.service.PostService;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;

import static pvt.mktech.petcare.social.entity.table.ActivityTableDef.ACTIVITY;

/**
 * 活动表 服务层实现。
 */
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    private final PostService postService;

    @Override
    public Activity createActivity(Activity activity) {
        activity.setAuditStatus(AuditStatusOfContent.PENDING);
        save(activity);
        return activity;
    }

    @Override
    public Page<Activity> getActivityList(Long pageNumber, Long pageSize, ActivityQueryRequest request) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(ACTIVITY.AUDIT_STATUS.eq(AuditStatusOfContent.APPROVED))
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
        return getOne(QueryWrapper.create()
                .where(ACTIVITY.ID.eq(id))
                .and(ACTIVITY.AUDIT_STATUS.eq(AuditStatusOfContent.APPROVED))
                .and(ACTIVITY.IS_DELETED.eq(false)));
    }

    @Override
    public Boolean joinActivity(Long userId, Long activityId) {
        Activity activity = getById(activityId);
        if (activity == null) {
            throw new BusinessException("404", "活动不存在");
        }
        if (!AuditStatusOfContent.APPROVED.equals(activity.getAuditStatus())) {
            throw new BusinessException("400", "活动未审核通过");
        }
        if (!StatusOfActivity.RECRUITING.equals(activity.getStatus())) {
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

        // 生成报名动态
        Post post = new Post();
        post.setUserId(userId);
        post.setPostType(TypeOfPost.ACTIVITY_JOIN);
        post.setActivityId(activityId);
        post.setEnabled(1);
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
        if (!AuditStatusOfContent.APPROVED.equals(activity.getAuditStatus())) {
            throw new BusinessException("400", "活动未审核通过");
        }
        if (activity.getCheckInEnabled() == null || activity.getCheckInEnabled() != 1) {
            throw new BusinessException("400", "活动未开启打卡");
        }
        // 创建打卡动态
        post.setUserId(userId);
        post.setPostType(TypeOfPost.ACTIVITY_CHECK);
        post.setActivityId(activityId);
        post.setEnabled(1);
        postService.save(post);

        activity.setCheckInCount((activity.getCheckInCount() == null ? 0 : activity.getCheckInCount()) + 1);
        updateById(activity);

        return post;
    }

    @Override
    public Page<Activity> pagePendingActivities(Long pageNumber, Long pageSize) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(ACTIVITY.AUDIT_STATUS.eq(AuditStatusOfContent.PENDING))
                .and(ACTIVITY.IS_DELETED.eq(false))
                .orderBy(ACTIVITY.CREATED_AT.desc());
        return page(Page.of(pageNumber, pageSize), queryWrapper);
    }

    @Override
    public boolean approveActivity(Long id) {
        return updateAuditStatus(id, AuditStatusOfContent.APPROVED);
    }

    @Override
    public boolean rejectActivity(Long id) {
        return updateAuditStatus(id, AuditStatusOfContent.REJECTED);
    }

    private boolean updateAuditStatus(Long id, AuditStatusOfContent auditStatus) {
        Activity activity = getById(id);
        if (activity == null || Boolean.TRUE.equals(activity.getIsDeleted())) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "活动不存在");
        }
        activity.setAuditStatus(auditStatus);
        return updateById(activity);
    }
}
