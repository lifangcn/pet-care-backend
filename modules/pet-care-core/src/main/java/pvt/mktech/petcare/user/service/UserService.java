package pvt.mktech.petcare.user.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.user.dto.response.AdminUserResponse;
import pvt.mktech.petcare.user.dto.request.UserUpdateRequest;
import pvt.mktech.petcare.shared.dto.CheckInStatsResponse;
import pvt.mktech.petcare.user.dto.response.UserResponse;
import pvt.mktech.petcare.user.entity.User;

import java.util.List;

/**
 * {@code @description} 用户服务接口
 * {@code @date} 2025/11/28 14:09
 *
 * @author Msichael
 */
public interface UserService extends IService<User> {
    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户响应对象
     */
    UserResponse getUserById(Long userId);

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户响应对象
     */
    UserResponse getUserByUsername(String username);

    /**
     * 更新用户信息
     *
     * @param userId  用户ID
     * @param request 用户更新请求对象
     * @return 更新后的用户响应对象
     */
    @Transactional
    UserResponse updateUser(Long userId, UserUpdateRequest request);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 存在返回true，否则返回false
     */
    boolean checkUsernameExists(String username);

    /**
     * 检查手机号是否存在
     *
     * @param phone 手机号
     * @return 存在返回true，否则返回false
     */
    boolean checkPhoneExists(String phone);

    /**
     * 获取活跃用户ID列表
     *
     * @return 活跃用户ID列表
     */
    List<Long> getActiveUserIds();

    /**
     * 用户签到
     *
     * @param userId 用户ID
     */
    Boolean checkIn(Long userId);

    /**
     * 获取用户签到统计信息
     *
     * @param userId 用户ID
     * @param year   年份
     * @param month  月份
     * @return 签到统计信息
     */
    CheckInStatsResponse getCheckInStats(Long userId, Long year, Long month);

    /**
     * 分页查询后台用户列表
     *
     * @param pageNumber 页码
     * @param pageSize 页大小
     * @return 用户分页结果
     * @author Michael Li
     * @since 2026-03-27
     */
    Page<AdminUserResponse> pageAdminUsers(Long pageNumber, Long pageSize);

    /**
     * 更新用户管理员身份
     *
     * @param userId 用户ID
     * @param isAdmin 是否授予管理员
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @Transactional
    boolean updateAdminRole(Long userId, Boolean isAdmin);

    /**
     * 更新用户启用状态
     *
     * @param userId 用户ID
     * @param enabled 启用状态
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @Transactional
    boolean updateEnabledStatus(Long userId, Integer enabled);

    /**
     * 根据用户ID获取管理员用户信息
     *
     * @param userId 用户ID
     * @return 管理员用户响应对象
     * @author Michael Li
     * @since 2026-03-28
     */
    AdminUserResponse getAdminUserById(Long userId);
}
