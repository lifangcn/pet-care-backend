package pvt.mktech.petcare.points.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import pvt.mktech.petcare.points.dto.request.PointsConsumeRequest;
import pvt.mktech.petcare.points.dto.request.PointsRecordQueryRequest;
import pvt.mktech.petcare.points.dto.response.PointsAccountResponse;
import pvt.mktech.petcare.points.entity.PointsAccount;
import pvt.mktech.petcare.points.entity.PointsRecord;

/**
 * {@code @description}: 积分核心服务接口
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
public interface PointsService extends IService<PointsAccount> {

    /**
     * 新用户注册赠送积分
     *
     * @param userId 用户ID
     */
    void grantRegisterPoints(Long userId);

    /**
     * 主动行为获取积分
     *
     * @param userId    用户ID
     * @param action    行为类型
     * @param bizId     关联业务ID
     * @return 实际获取积分数
     */
    Integer earnByAction(Long userId, pvt.mktech.petcare.points.entity.codelist.PointsActionType action, Long bizId);

    /**
     * 质量行为获取积分（被点赞/被评论）
     *
     * @param authorId  内容作者ID
     * @param action    行为类型
     * @param contentId 内容ID
     * @param interactUserId 互动用户ID
     * @return 实际获取积分数（首次互动返回积分值，重复互动返回0）
     */
    Integer earnByQuality(Long authorId, pvt.mktech.petcare.points.entity.codelist.PointsActionType action,
                         Long contentId, Long interactUserId);

    /**
     * 消耗积分
     *
     * @param request  消耗请求
     * @return true-成功，false-失败
     */
    boolean consume(PointsConsumeRequest request);

    /**
     * 获取用户积分账户信息
     *
     * @param userId 用户ID
     * @return 积分账户信息
     */
    PointsAccountResponse getAccount(Long userId);

    /**
     * 分页查询积分流水
     *
     * @param userId 用户ID
     * @param pageNumber 页码
     * @param pageSize 页大小
     * @param request 查询条件
     * @return 分页结果
     */
    Page<PointsRecord> pageRecords(Long userId, Long pageNumber, Long pageSize, PointsRecordQueryRequest request);
}
