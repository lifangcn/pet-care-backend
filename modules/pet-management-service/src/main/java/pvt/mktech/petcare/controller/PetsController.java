package pvt.mktech.petcare.controller;

import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.entity.Pets;
import pvt.mktech.petcare.service.PetsService;


import java.util.List;

/**
 * 宠物表 控制层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-02
 */
@RestController
@RequestMapping("/v1/pets")
@RequiredArgsConstructor
public class PetsController {

    private final PetsService petsService;


    @PostMapping("listMyPets")
    @Operation(
            summary = "查询当前用户的所有宠物列表",
            description = "用户信息基于前端请求头的Authorization获取用户信息，避免明文传递用户私密信息"
    )
    public Result<List<Pets>> listMyPets(@Parameter(description = "custom uuid token", required = true)
                                 @RequestHeader("Authorization") String token) {
        // TODO 对于微服务中使用到token的地方，进行通用处理
        return Result.ok(petsService.findByUserId(token));
    }

    /**
     * 保存宠物表。
     *
     * @param pets 宠物表
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    @Operation(
            summary = "查询当前用户的所有宠物列表",
            description = "用户信息基于前端请求头的Authorization获取用户信息，避免明文传递用户私密信息"
    )
    public Result<Pets> save(@Parameter(description = "custom uuid token", required = true)
                            @RequestHeader("Authorization") String token,
                       @RequestBody Pets pets) {
        return Result.ok(petsService.save(token, pets));
    }

    /**
     * 根据主键删除宠物表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return petsService.removeById(id);
    }

    /**
     * 根据主键更新宠物表。
     *
     * @param pets 宠物表
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody Pets pets) {
        return petsService.updateById(pets);
    }


    /**
     * 查询所有宠物表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<Pets> list() {
        return petsService.list();
    }

    /**
     * 根据主键获取宠物表。
     *
     * @param id 宠物表主键
     * @return 宠物表详情
     */
    @GetMapping("getInfo/{id}")
    public Pets getInfo(@PathVariable Long id) {
        return petsService.getById(id);
    }

    /**
     * 分页查询宠物表。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<Pets> page(Page<Pets> page) {
        return petsService.page(page);
    }

}
