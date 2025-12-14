package pvt.mktech.petcare.controller;

import com.mybatisflex.core.paginate.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.entity.PetVaccinations;
import pvt.mktech.petcare.service.PetVaccinationsService;

import java.util.List;

/**
 * 宠物疫苗记录表 控制层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-08
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/petVaccinations")
public class PetVaccinationsController {

    private final PetVaccinationsService petVaccinationsService;

    /**
     * 保存宠物疫苗记录表。
     *
     * @param petVaccinations 宠物疫苗记录表
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody PetVaccinations petVaccinations) {
        return petVaccinationsService.save(petVaccinations);
    }

    /**
     * 根据主键删除宠物疫苗记录表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return petVaccinationsService.removeById(id);
    }

    /**
     * 根据主键更新宠物疫苗记录表。
     *
     * @param petVaccinations 宠物疫苗记录表
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody PetVaccinations petVaccinations) {
        return petVaccinationsService.updateById(petVaccinations);
    }

    /**
     * 查询所有宠物疫苗记录表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<PetVaccinations> list() {
        return petVaccinationsService.list();
    }

    /**
     * 根据主键获取宠物疫苗记录表。
     *
     * @param id 宠物疫苗记录表主键
     * @return 宠物疫苗记录表详情
     */
    @GetMapping("getInfo/{id}")
    public PetVaccinations getInfo(@PathVariable Long id) {
        return petVaccinationsService.getById(id);
    }

    /**
     * 分页查询宠物疫苗记录表。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<PetVaccinations> page(Page<PetVaccinations> page) {
        return petVaccinationsService.page(page);
    }

}
