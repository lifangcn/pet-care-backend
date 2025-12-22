package pvt.mktech.petcare.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pvt.mktech.petcare.common.context.UserContext;
import pvt.mktech.petcare.common.dto.UserInfoDto;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.dto.response.ResultCode;
import pvt.mktech.petcare.common.util.MinioUtil;
import pvt.mktech.petcare.entity.Pet;
import pvt.mktech.petcare.service.PetService;

import java.util.List;

/**
 * 宠物表 控制层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-02
 */
@Tag(name = "宠物管理", description = "宠物信息管理相关接口")
@RestController
@RequestMapping("/pet")
@Slf4j
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;
    private final MinioUtil minioUtil;

    /**
     * 根据主键删除宠物表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @GetMapping("/info/{id}")
    public Result<Pet> info(@PathVariable("id") Long id) {
        return Result.success(petService.getById(id));
    }

    @PostMapping("/list")
    @Operation(
            summary = "查询当前用户的所有宠物列表",
            description = "用户信息基于前端请求头的Authorization获取用户信息，避免明文传递用户私密信息"
    )
    public Result<List<Pet>> listMyPets() {
        UserInfoDto userInfo = UserContext.getUserInfo();
        return Result.success(petService.findByUserId(userInfo.getUserId()));
    }

    /**
     * 保存宠物表。
     *
     * @param pet 宠物表
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("/save")
    @Operation(
            summary = "查询当前用户的所有宠物列表",
            description = "用户信息基于前端请求头的Authorization获取用户信息，避免明文传递用户私密信息"
    )
    public Result<Pet> save(@RequestBody Pet pet) {
        return Result.success(petService.savePet(pet));
    }

    /**
     * 根据主键删除宠物表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("/remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return petService.removeById(id);
    }

    /**
     * 根据主键更新宠物表。
     *
     * @param pet 宠物表
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("/update")
    public boolean update(@RequestBody Pet pet) {
        return petService.updateById(pet);
    }

    @Operation(summary = "上传宠物头像")
    @PostMapping(value = "/{petId}/avatar", consumes = "multipart/form-data")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, @PathVariable("petId") Long petId) {
        try {
            Long userId = UserContext.getUserInfo().getUserId();
            // 1. 权限验证（只能修改自己的头像）
            if (userId == null) {
                return Result.error(ResultCode.UNAUTHORIZED, "无权修改其他用户头像");
            }
            // 上传头像到MinIO
            String avatarUrl = minioUtil.uploadAvatar(file, userId);
            return Result.success(avatarUrl);
        } catch (RuntimeException e) {
            log.error("头像上传失败: ", e);
            return Result.error(ResultCode.FAILED, e.getMessage());
        }
    }
}
