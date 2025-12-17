package pvt.mktech.petcare.service;

import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.dto.request.UserUpdateRequest;
import pvt.mktech.petcare.dto.response.UserResponse;

/**
 * {@code @description} 用户服务接口
 * {@code @date} 2025/11/28 14:09
 *
 * @author Msichael
 */
public interface UserService {
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
     * 修改用户密码
     *
     * @param userId      用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    @Transactional
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 存在返回true，否则返回false
     */
    boolean checkUsernameExists(String username);

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱地址
     * @return 存在返回true，否则返回false
     */
    boolean checkEmailExists(String email);

    /**
     * 检查手机号是否存在
     *
     * @param phone 手机号
     * @return 存在返回true，否则返回false
     */
    boolean checkPhoneExists(String phone);
}
