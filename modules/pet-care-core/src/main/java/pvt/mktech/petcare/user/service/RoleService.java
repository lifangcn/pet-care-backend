package pvt.mktech.petcare.user.service;

import com.mybatisflex.core.service.IService;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.user.entity.Role;

import java.util.List;

/**
 * 角色服务接口
 * {@code @date}: 2026/03/23
 * @author Michael Li
 */
public interface RoleService extends IService<Role> {
    /**
     * 根据角色ID获取角色信息
     *
     * @param roleId 角色ID
     * @return 角色实体
     */
    Role getRoleById(Long roleId);

    /**
     * 根据角色编码获取角色信息
     *
     * @param roleCode 角色编码
     * @return 角色实体
     */
    Role getRoleByCode(String roleCode);

    /**
     * 获取所有角色列表
     *
     * @return 角色列表
     */
    List<Role> getAllRoles();

    /**
     * 创建角色
     *
     * @param role 角色实体
     * @return 创建后的角色实体
     */
    @Transactional
    Role createRole(Role role);

    /**
     * 更新角色
     *
     * @param role 角色实体
     * @return 更新后的角色实体
     */
    @Transactional
    Role updateRole(Role role);

    /**
     * 删除角色
     *
     * @param roleId 角色ID
     */
    @Transactional
    void deleteRole(Long roleId);

    /**
     * 根据用户ID获取角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> getRolesByUserId(Long userId);
}