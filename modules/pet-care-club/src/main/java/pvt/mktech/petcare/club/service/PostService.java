package pvt.mktech.petcare.club.service;

import com.mybatisflex.core.service.IService;
import com.mybatisflex.core.paginate.Page;
import pvt.mktech.petcare.club.dto.request.PostQueryRequest;
import pvt.mktech.petcare.club.entity.Post;

/**
 * 动态表 服务层。
 */
public interface PostService extends IService<Post> {

    /**
     * 发布动态
     */
    Post savePost(Post post);

    /**
     * 动态列表
     */
    Page<Post> getPostList(PostQueryRequest request);

    /**
     * 动态详情
     */
    Post getPostDetail(Long id);

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
}
