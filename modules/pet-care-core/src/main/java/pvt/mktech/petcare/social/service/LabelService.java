package pvt.mktech.petcare.social.service;

import com.mybatisflex.core.service.IService;
import pvt.mktech.petcare.social.entity.Label;

import java.util.List;

/**
 * 标签表 服务层。
 */
public interface LabelService extends IService<Label> {

    /**
     * 根据标签类型获取标签列表
     * @param type 标签类型
     * @return 标签列表
     */
    List<Label> listLabelByType(Integer type);

    /**
     * 获取热门标签
     */
    List<Label> getHotLabels();

    /**
     * 根据关键词获取标签建议
     */
    List<Label> suggestLabels(String keyword);

    /**
     * 增加使用次数
     */
    void incrementUseCount(Long labelId);
}
