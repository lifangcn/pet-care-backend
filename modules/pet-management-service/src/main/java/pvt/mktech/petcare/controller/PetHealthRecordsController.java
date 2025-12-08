package pvt.mktech.petcare.controller;

import com.mybatisflex.core.paginate.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import pvt.mktech.petcare.entity.PetHealthRecords;
import pvt.mktech.petcare.service.PetHealthRecordsService;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 宠物健康记录表 控制层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-08
 */
@RestController
@RequestMapping("/petHealthRecords")
public class PetHealthRecordsController {

    @Autowired
    private PetHealthRecordsService petHealthRecordsService;

    /**
     * 保存宠物健康记录表。
     *
     * @param petHealthRecords 宠物健康记录表
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody PetHealthRecords petHealthRecords) {
        return petHealthRecordsService.save(petHealthRecords);
    }

    /**
     * 根据主键删除宠物健康记录表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return petHealthRecordsService.removeById(id);
    }

    /**
     * 根据主键更新宠物健康记录表。
     *
     * @param petHealthRecords 宠物健康记录表
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody PetHealthRecords petHealthRecords) {
        return petHealthRecordsService.updateById(petHealthRecords);
    }

    /**
     * 查询所有宠物健康记录表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<PetHealthRecords> list() {
        return petHealthRecordsService.list();
    }

    /**
     * 根据主键获取宠物健康记录表。
     *
     * @param id 宠物健康记录表主键
     * @return 宠物健康记录表详情
     */
    @GetMapping("getInfo/{id}")
    public PetHealthRecords getInfo(@PathVariable Long id) {
        return petHealthRecordsService.getById(id);
    }

    /**
     * 分页查询宠物健康记录表。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<PetHealthRecords> page(Page<PetHealthRecords> page) {
        return petHealthRecordsService.page(page);
    }

}
