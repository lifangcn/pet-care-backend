package pvt.mktech.petcare.club.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import pvt.mktech.petcare.club.handler.StringListTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动表 实体类。
 */
@Table("tb_activity")
@Data
public class Activity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 创建者ID
     */
    private Long userId;

    /**
     * 活动标题
     */
    private String title;

    /**
     * 活动描述
     */
    private String description;

    /**
     * 封面图
     */
    private String coverImage;

    /**
     * 1-线上活动 2-线下聚会
     */
    private Integer activityType;

    /**
     * 活动时间
     */
    private LocalDateTime activityTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 线下地址
     */
    private String address;

    /**
     * 线上链接
     */
    private String onlineLink;

    /**
     * 最大参与人数 0-不限
     */
    private Integer maxParticipants;

    /**
     * 当前参与人数
     */
    private Integer currentParticipants;

    /**
     * 1-招募中 2-进行中 3-已结束
     */
    private Integer status;

    /**
     * 活动标签数组
     */
    @Column(typeHandler = StringListTypeHandler.class)
    private List<String> labels;

    /**
     * 是否开启打卡 0-否 1-是
     */
    private Integer checkinEnabled;

    /**
     * 打卡人数
     */
    private Integer checkinCount;

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
}
