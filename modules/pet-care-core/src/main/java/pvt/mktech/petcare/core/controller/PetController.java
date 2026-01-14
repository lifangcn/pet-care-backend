package pvt.mktech.petcare.core.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.storage.OssTemplate;
import pvt.mktech.petcare.common.usercache.UserContext;
import pvt.mktech.petcare.core.dto.request.HealthRecordQueryRequest;
import pvt.mktech.petcare.core.dto.request.HealthRecordSaveRequest;
import pvt.mktech.petcare.core.entity.HealthRecord;
import pvt.mktech.petcare.core.entity.Pet;
import pvt.mktech.petcare.core.service.HealthRecordService;
import pvt.mktech.petcare.core.service.PetService;

import java.time.LocalDateTime;
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

    private final HealthRecordService healthRecordService;
    private final PetService petService;
    private final OssTemplate ossTemplate;

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
        return Result.success(petService.findByUserId(UserContext.getUserId()));
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
        // 上传头像到MinIO
        String avatarUrl = ossTemplate.uploadAvatar(file, petId);
        return Result.success(avatarUrl);
    }

    /* 健康记录 */

    /**
     * 创建健康记录表。
     *
     * @param saveRequest 健康记录保存请求
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("/{petId}/health-record")
    public boolean saveHealthRecord(@RequestBody HealthRecordSaveRequest saveRequest) {
        HealthRecord healthRecord = new HealthRecord();
        BeanUtil.copyProperties(saveRequest, healthRecord);
        healthRecord.setUserId(UserContext.getUserId());
        // TODO 扩展点，根据已经填写的健康记录分类，发送到消息队列，消息队列将任务推送给AI，调用对应的诊断Tool。返回一些有意义的提醒通知
        return healthRecordService.save(healthRecord);
    }

    /**
     * 根据主键删除健康记录表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("/{petId}/health-record/{id}")
    public boolean deleteHealthRecord(@PathVariable("petId") Long petId,
                                      @PathVariable("id") Long id) {
        return healthRecordService.removeById(id);
    }

    /**
     * 根据主键更新健康记录表。
     *
     * @param saveRequest 健康记录保存请求
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("/{petId}/health-record/{id}")
    public boolean updateHealthRecord(@PathVariable("petId") Long petId,
                                      @PathVariable("id") Long id,
                                      @RequestBody HealthRecordSaveRequest saveRequest) {
        HealthRecord healthRecord = new HealthRecord();
        BeanUtil.copyProperties(saveRequest, healthRecord);
        healthRecord.setId(id);
        return healthRecordService.updateById(healthRecord);
    }

    /**
     * 分页查询健康记录表。
     *
     * @param petId      宠物ID
     * @param recordType 记录类型
     * @param startDate  开始时间
     * @param endDate    结束时间
     * @param pageNumber 页码
     * @param pageSize   页大小
     * @return 分页查询结果
     */
    @GetMapping("/{petId}/health-record/page")
    public Page<HealthRecord> pageHealthRecord(@PathVariable("petId") Long petId,
                                               @RequestParam(value = "recordType", required = false) String recordType,
                                               @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
                                               @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate,
                                               @RequestParam(value = "pageNumber", defaultValue = "1") Long pageNumber,
                                               @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        HealthRecordQueryRequest request = new HealthRecordQueryRequest(petId, recordType, startDate, endDate, pageNumber, pageSize);
        return healthRecordService.findPageByQueryRequest(request);
    }
}
