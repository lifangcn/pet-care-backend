package pvt.mktech.petcare.service;

import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.dto.request.UserUpdateRequest;
import pvt.mktech.petcare.dto.response.UserResponse;

public interface UserService {
    UserResponse getUserById(Long userId);

    UserResponse getUserByUsername(String username);

    @Transactional
    UserResponse updateUser(Long userId, UserUpdateRequest request);

    @Transactional
    void updateLastLogin(Long userId, String loginIp);

    @Transactional
    void changePassword(Long userId, String oldPassword, String newPassword);

    boolean checkUsernameExists(String username);

    boolean checkEmailExists(String email);

    boolean checkPhoneExists(String phone);
}
