package pvt.mktech.petcare.social.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import pvt.mktech.petcare.social.entity.codelist.StatusOfActivity;
import pvt.mktech.petcare.social.entity.codelist.TypeOfActivity;
import pvt.mktech.petcare.social.handler.StringListTypeHandler;

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
     * 类型：ONLINE-线上活动 OFFLINE-线下聚会
     */
    private TypeOfActivity activityType;

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
     * 状态：RECRUITING-招募中 ONGOING-进行中 ENDED-已结束
     */
    private StatusOfActivity status;

    /**
     * 活动标签数组
     */
    @Column(typeHandler = StringListTypeHandler.class)
    private List<String> labels;

    /**
     * 是否开启打卡 0-否 1-是
     */
    private Integer checkInEnabled;

    /**
     * 打卡人数
     */
    private Integer checkInCount;

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
    private Boolean isDeleted;

    /**
     * 删除时间
     */
    @Column(value = "deleted_at")
    private LocalDateTime deletedAt;
}
