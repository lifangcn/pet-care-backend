package pvt.mktech.petcare.social.service;

import com.mybatisflex.core.service.IService;
import com.mybatisflex.core.paginate.Page;
import pvt.mktech.petcare.social.dto.request.PostQueryRequest;
import pvt.mktech.petcare.social.dto.request.PostSaveRequest;
import pvt.mktech.petcare.social.dto.response.PostDetailResponse;
import pvt.mktech.petcare.social.entity.Post;

import java.util.List;

/**
 * 动态表 服务层。
 */
public interface PostService extends IService<Post> {

    /**
     * 发布动态
     */
    Post savePost(PostSaveRequest request);

    /**
     * 根据查询条件分页查询。
     *
     * @param pageNumber 页码
     * @param pageSize   页大小
     * @param request    查询条件
     * @return 分页查询结果
     */
    Page<Post> findPageByQueryRequest(Long pageNumber, Long pageSize, PostQueryRequest request);


    /**
     * 动态详情
     */
    PostDetailResponse getPostDetail(Long id);

    /**
     * 编辑动态
     */
    boolean updatePost(Long id, Post post);

    /**
     * 删除动态
     */
    boolean deletePost(Long id);

    /**
     * 增加浏览量
     */
    void incrementViewCount(Long id);

    /**
     * 获取活动报名列表
     *
     * @param activityId 活动ID
     * @return 报名信息列表
     */
    List<Post> listParticipantsByActivityId(Long activityId);

    /**
     * 判断用户是否已报名
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     * @return true-已报名，false-未报名
     */
    Boolean hasJoinActivity(Long userId, Long activityId);

    /**
     * 点赞/取消点赞
     *
     * @param postId 动态ID
     * @return true=点赞, false=取消点赞
     */
    boolean toggleLike(Long postId);

    /**
     * 评分
     *
     * @param postId 动态ID
     * @param rating 评分值 1-5
     * @return 是否成功
     */
    boolean rate(Long postId, Integer rating);
}
