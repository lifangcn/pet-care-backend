package pvt.mktech.petcare.social.service;

import com.mybatisflex.core.service.IService;
import pvt.mktech.petcare.social.entity.Label;
import pvt.mktech.petcare.social.entity.PostLabel;

import java.util.List;

/**
 * {@code @description}: 动态标签关联 服务层
 * {@code @date}: 2025-01-22
 * {@code @author}: Michael
 */
public interface PostLabelService extends IService<PostLabel> {

    /**
     * 保存动态标签关联
     * @param postId 动态ID
     * @param labelIds 标签ID列表
     */
    void savePostLabels(Long postId, List<Long> labelIds);

    /**
     * 根据动态ID查询标签列表
     * @param postId 动态ID
     * @return 标签列表
     */
    List<Label> listLabelsByPostId(Long postId);

    /**
     * 删除动态的所有标签关联
     * @param postId 动态ID
     */
    void deleteByPostId(Long postId);
}
