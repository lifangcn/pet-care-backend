package pvt.mktech.petcare.admin.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.web.UserContext;
import pvt.mktech.petcare.user.entity.Role;
import pvt.mktech.petcare.user.service.RoleService;

import java.util.List;

/**
 * 管理员权限校验切面
 *
 * @author Michael Li
 * @since 2026-03-27
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequireAdminAspect {

    private static final String ADMIN_ROLE_CODE = "admin";

    private final RoleService roleService;

    @Around("@within(pvt.mktech.petcare.admin.security.RequireAdmin) || @annotation(pvt.mktech.petcare.admin.security.RequireAdmin)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        List<Role> roles = roleService.getRolesByUserId(userId);
        boolean isAdmin = roles.stream()
                .filter(role -> role.getStatus() != null && role.getStatus() == 1)
                .filter(role -> role.getIsDeleted() == null || !role.getIsDeleted())
                .anyMatch(role -> ADMIN_ROLE_CODE.equalsIgnoreCase(role.getRoleCode()));
        if (!isAdmin) {
            log.warn("管理员权限校验失败: userId={}", userId);
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return joinPoint.proceed();
    }
}
