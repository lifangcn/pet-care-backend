package pvt.mktech.petcare.social.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import pvt.mktech.petcare.social.handler.LocationInfoTypeHandler;
import pvt.mktech.petcare.social.handler.MediaUrlListTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 动态表 实体类。
 */
@Table("tb_post")
@Data
public class Post implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 发布者ID
     */
    private Long userId;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容描述
     */
    private String content;

    /**
     * 1-好物分享 2-服务推荐 3-地点推荐 4-日常分享 5-活动打卡 6-活动报名
     */
    private Integer postType;

    /**
     * 图片/视频URL数组
     */
    @Column(typeHandler = MediaUrlListTypeHandler.class)
    private List<MediaUrl> mediaUrls;

    /**
     * 外部链接（商品、服务、地图）
     */
    private String externalLink;

    /**
     * 地点信息
     */
    @Column(typeHandler = LocationInfoTypeHandler.class)
    private LocationInfo locationInfo;

    /**
     * 价格区间（如：100-200元）
     */
    private String priceRange;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评分次数
     */
    private Integer ratingCount;

    /**
     * 评分总分
     */
    private Integer ratingTotal;

    /**
     * 平均分
     */
    private BigDecimal ratingAvg;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 1-正常 2-隐藏
     */
    private Integer status;

    /**
     * 关联的活动ID
     */
    private Long activityId;

    /**
     * 创建时间
     */
    @Column(value = "created_at", onInsertValue = "CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(value = "updated_at", onInsertValue = "CURRENT_TIMESTAMP", onUpdateValue = "CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除：0-正常，1-已删除
     */
    @Column(value = "is_deleted", onInsertValue = "0")
    private Integer isDeleted;

    /**
     * 删除时间
     */
    @Column(value = "deleted_at")
    private LocalDateTime deletedAt;
}
