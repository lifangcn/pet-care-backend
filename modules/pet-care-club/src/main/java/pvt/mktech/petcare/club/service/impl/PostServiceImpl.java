package pvt.mktech.petcare.club.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.club.dto.request.PostQueryRequest;
import pvt.mktech.petcare.club.entity.Post;
import pvt.mktech.petcare.club.mapper.PostMapper;
import pvt.mktech.petcare.club.service.PostService;

import static pvt.mktech.petcare.club.entity.table.PostTableDef.POST;

/**
 * 动态表 服务层实现。
 */
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    @Override
    public Post savePost(Post post) {
        save(post);
        // TODO 扩展点：处理标签关联、发送通知等
        return post;
    }

    @Override
    public Page<Post> getPostList(PostQueryRequest request) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(POST.STATUS.eq(1))
                .orderBy(POST.CREATED_AT.desc());

        if (request.getPostType() != null) {
            queryWrapper.and(POST.POST_TYPE.eq(request.getPostType()));
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

        return page(Page.of(request.getPageNumber(), request.getPageSize()), queryWrapper);
    }

    @Override
    public Post getPostDetail(Long id) {
        Post post = getById(id);
        if (post != null) {
            incrementViewCount(id);
        }
        return post;
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
}
