package pvt.mktech.petcare.service;

import jakarta.servlet.http.HttpSession;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.dto.LoginInfoDto;
import pvt.mktech.petcare.dto.request.LoginRequest;

/**
 * {@code @description}: 鉴权接口
 * {@code @date}: 2025/11/28 14:49
 *
 * @author Michael
 */
public interface AuthService {

    /**
     * 发送验证码到指定手机号
     * @param phone 手机号码
     * @param httpSession HTTP会话对象
     * @return 响应实体，包含发送结果信息
     */
    Result<String> sendCode(String phone, HttpSession httpSession);
    
    /**
     * 用户登录验证，如果账号不存在，则创建用户
     * @param request 登录请求数据传输对象
     * @return 登录响应数据传输对象
     */
    Result<LoginInfoDto> login(LoginRequest request);

    /**
     * 刷新令牌
     *
     * @param dto 登录信息数据传输对象
     * @return 登录信息数据传输对象
     */
    LoginInfoDto refreshToken(LoginInfoDto dto);

    /**
     * 退出登录
     */
    void logout(LoginInfoDto dto);
}
