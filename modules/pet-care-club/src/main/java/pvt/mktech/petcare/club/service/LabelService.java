package pvt.mktech.petcare.club.service;

import com.mybatisflex.core.service.IService;
import pvt.mktech.petcare.club.entity.Label;

import java.util.List;

/**
 * 标签表 服务层。
 */
public interface LabelService extends IService<Label> {

    /**
     * 标签列表
     */
    List<Label> getLabelList(Integer type);

    /**
     * 热门标签
     */
    List<Label> getHotLabels();

    /**
     * 标签建议
     */
    List<Label> suggestLabels(String keyword);

    /**
     * 增加使用次数
     */
    void incrementUseCount(Long labelId);
}
