package pvt.mktech.petcare.points.controller.admin;

import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pvt.mktech.petcare.admin.security.RequireAdmin;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.points.dto.request.PointsCouponTemplateRequest;
import pvt.mktech.petcare.points.entity.PointsCouponTemplate;
import pvt.mktech.petcare.points.entity.PointsRecord;
import pvt.mktech.petcare.points.service.PointsCouponService;
import pvt.mktech.petcare.points.service.PointsService;

import java.util.List;

/**
 * 后台积分券管理控制器
 *
 * @author Michael Li
 * @since 2026-03-27
 */
@Tag(name = "后台积分券管理", description = "后台积分券管理相关接口")
@RestController
@RequireAdmin
@RequestMapping("/admin/points/coupon")
@RequiredArgsConstructor
public class AdminPointsCouponController {

    private final PointsCouponService pointsCouponService;
    private final PointsService pointsService;

    /**
     * 创建积分券模板
     *
     * @param request 创建模板请求
     * @return 模板ID
     * @author Michael Li
     * @since 2026-03-27
     */
    @PostMapping("/template")
    @Operation(summary = "创建积分券模板")
    public Result<Long> createTemplate(@Valid @RequestBody PointsCouponTemplateRequest request) {
        Long templateId = pointsCouponService.createTemplate(request);
        return Result.success(templateId);
    }

    /**
     * 查询积分券模板列表
     *
     * @param pageNumber 页码
     * @param pageSize 页大小
     * @return 模板分页结果
     * @author Michael Li
     * @since 2026-03-27
     */
    @GetMapping("/templates")
    @Operation(summary = "查询积分券模板列表")
    public Result<Page<PointsCouponTemplate>> getTemplates(@RequestParam(value = "pageNumber", defaultValue = "1") Long pageNumber,
                                                           @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        return Result.success(pointsCouponService.pageTemplates(pageNumber, pageSize));
    }

    /**
     * 编辑积分券模板
     *
     * @param id 模板ID
     * @param request 编辑模板请求
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @PutMapping("/template/{id}")
    @Operation(summary = "编辑积分券模板")
    public Result<Boolean> updateTemplate(@PathVariable("id") Long id,
                                          @Valid @RequestBody PointsCouponTemplateRequest request) {
        return Result.success(pointsCouponService.updateTemplate(id, request));
    }

    /**
     * 批量发放积分券
     *
     * @param id 模板ID
     * @param userIds 用户ID列表
     * @return 是否成功
     * @author Michael Li
     * @since 2026-03-27
     */
    @PostMapping("/template/{id}/issue")
    @Operation(summary = "批量发放积分券")
    public Result<Boolean> issueCoupons(@PathVariable("id") Long id,
                                        @RequestBody List<Long> userIds) {
        return Result.success(pointsCouponService.issueCoupons(id, userIds));
    }

    /**
     * 查询积分流水
     *
     * @param pageNumber 页码
     * @param pageSize 页大小
     * @return 积分流水分页结果
     * @author Michael Li
     * @since 2026-03-27
     */
    @GetMapping("/records")
    @Operation(summary = "查询积分流水")
    public Result<Page<PointsRecord>> getRecords(@RequestParam(value = "pageNumber", defaultValue = "1") Long pageNumber,
                                                  @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        return Result.success(pointsService.pageRecords(pageNumber, pageSize));
    }
}
