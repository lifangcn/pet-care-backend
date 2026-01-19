package pvt.mktech.petcare.club.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import pvt.mktech.petcare.club.dto.request.ActivityQueryRequest;
import pvt.mktech.petcare.club.entity.Activity;
import pvt.mktech.petcare.club.entity.Post;

import java.util.List;

/**
 * 活动表 服务层。
 */
public interface ActivityService extends IService<Activity> {

    /**
     * 创建活动
     */
    Activity createActivity(Activity activity);

    /**
     * 活动列表
     */
    Page<Activity> getActivityList(ActivityQueryRequest request);

    /**
     * 活动详情
     */
    Activity getActivityDetail(Long id);

    /**
     * 报名活动
     */
    boolean joinActivity(Long userId, Long activityId);

    /**
     * 活动打卡
     */
    Post checkinActivity(Long userId, Long activityId, Post post);

    /**
     * 获取打卡动态
     */
    List<Post> getActivityCheckins(Long activityId);
}
