package pvt.mktech.petcare.controller;

import com.mybatisflex.core.paginate.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.entity.HealthRecord;
import pvt.mktech.petcare.service.HealthRecordService;

import java.util.List;

/**
 * 宠物健康记录表 控制层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-08
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/healthRecord")
public class HealthRecordController {


    private final HealthRecordService healthRecordService;

    /**
     * 保存宠物健康记录表。
     *
     * @param healthRecord 宠物健康记录表
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody HealthRecord healthRecord) {
        return healthRecordService.save(healthRecord);
    }

    /**
     * 根据主键删除宠物健康记录表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable Long id) {
        return healthRecordService.removeById(id);
    }

    /**
     * 根据主键更新宠物健康记录表。
     *
     * @param healthRecord 宠物健康记录表
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody HealthRecord healthRecord) {
        return healthRecordService.updateById(healthRecord);
    }

    /**
     * 查询所有宠物健康记录表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<HealthRecord> list() {
        return healthRecordService.list();
    }

    /**
     * 根据主键获取宠物健康记录表。
     *
     * @param id 宠物健康记录表主键
     * @return 宠物健康记录表详情
     */
    @GetMapping("getInfo/{id}")
    public HealthRecord getInfo(@PathVariable Long id) {
        return healthRecordService.getById(id);
    }

    /**
     * 分页查询宠物健康记录表。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<HealthRecord> page(Page<HealthRecord> page) {
        return healthRecordService.page(page);
    }

}
