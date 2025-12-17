package pvt.mktech.petcare.controller;

import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.common.context.UserContext;
import pvt.mktech.petcare.common.dto.UserInfoDto;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.entity.Pet;
import pvt.mktech.petcare.service.PetService;


import java.util.List;

/**
 * 宠物表 控制层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-02
 */
@RestController
@RequestMapping("/pet")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;


    @PostMapping("listMyPets")
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
    @PostMapping("save")
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
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return petService.removeById(id);
    }

    /**
     * 根据主键更新宠物表。
     *
     * @param pet 宠物表
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody Pet pet) {
        return petService.updateById(pet);
    }


    /**
     * 查询所有宠物表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<Pet> list() {
        return petService.list();
    }

    /**
     * 根据主键获取宠物表。
     *
     * @param id 宠物表主键
     * @return 宠物表详情
     */
    @GetMapping("getInfo/{id}")
    public Pet getInfo(@PathVariable Long id) {
        return petService.getById(id);
    }

    /**
     * 分页查询宠物表。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<Pet> page(Page<Pet> page) {
        return petService.page(page);
    }

}
