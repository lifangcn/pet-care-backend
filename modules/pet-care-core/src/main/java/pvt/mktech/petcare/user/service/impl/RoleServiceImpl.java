package pvt.mktech.petcare.user.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.user.entity.Role;
import pvt.mktech.petcare.user.entity.UserRole;
import pvt.mktech.petcare.user.mapper.RoleMapper;
import pvt.mktech.petcare.user.mapper.UserRoleMapper;
import pvt.mktech.petcare.user.service.RoleService;

import java.util.List;
import java.util.stream.Collectors;

import static pvt.mktech.petcare.user.entity.table.RoleTableDef.ROLE;
import static pvt.mktech.petcare.user.entity.table.UserRoleTableDef.USER_ROLE;

/**
 * 角色服务实现类
 * {@code @date}: 2026/03/23
 * @author Michael Li
 */
@Slf4j
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Resource
    private RoleMapper roleMapper;
    @Resource
    private UserRoleMapper userRoleMapper;

    @Override
    public Role getRoleById(Long roleId) {
        Role role = getOne(ROLE.ID.eq(roleId));
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    @Override
    public Role getRoleByCode(String roleCode) {
        Role role = getOne(ROLE.ROLE_CODE.eq(roleCode));
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    @Override
    public List<Role> getAllRoles() {
        return list(QueryWrapper.create().where(ROLE.IS_DELETED.eq(false)).orderBy(ROLE.SORT.asc()));
    }

    @Transactional
    @Override
    public Role createRole(Role role) {
        // 检查角色编码是否已存在
        if (exists(ROLE.ROLE_CODE.eq(role.getRoleCode()))) {
            throw new BusinessException(ErrorCode.ROLE_CODE_ALREADY_EXISTS);
        }
        save(role);
        log.info("创建角色成功: roleCode={}", role.getRoleCode());
        return role;
    }

    @Transactional
    @Override
    public Role updateRole(Role role) {
        // 检查角色是否存在
        if (!exists(ROLE.ID.eq(role.getId()))) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        // 检查角色编码是否已被其他角色使用
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(ROLE.ROLE_CODE.eq(role.getRoleCode()))
                .and(ROLE.ID.ne(role.getId()));
        if (roleMapper.selectCountByQuery(queryWrapper) > 0) {
            throw new BusinessException(ErrorCode.ROLE_CODE_ALREADY_EXISTS);
        }
        updateById(role);
        log.info("更新角色成功: roleId={}", role.getId());
        return role;
    }

    @Transactional
    @Override
    public void deleteRole(Long roleId) {
        // 检查角色是否存在
        if (!exists(ROLE.ID.eq(roleId))) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        // 检查角色是否被使用
        QueryWrapper userRoleQuery = QueryWrapper.create().where(USER_ROLE.ROLE_ID.eq(roleId));
        if (userRoleMapper.selectCountByQuery(userRoleQuery) > 0) {
            throw new BusinessException(ErrorCode.ROLE_IN_USE);
        }
        removeById(roleId);
        log.info("删除角色成功: roleId={}", roleId);
    }

    @Override
    public List<Role> getRolesByUserId(Long userId) {
        // 查询用户关联的角色ID
        List<UserRole> userRoles = userRoleMapper.selectListByQuery(QueryWrapper.create().where(USER_ROLE.USER_ID.eq(userId)));
        if (userRoles.isEmpty()) {
            return List.of();
        }
        // 获取角色ID列表
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        // 查询角色信息
        return listByIds(roleIds);
    }
}