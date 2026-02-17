package pvt.mktech.petcare.points.controller;

import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.web.UserContext;
import pvt.mktech.petcare.points.dto.request.PointsConsumeRequest;
import pvt.mktech.petcare.points.dto.request.PointsCouponQueryRequest;
import pvt.mktech.petcare.points.dto.request.PointsRecordQueryRequest;
import pvt.mktech.petcare.points.dto.response.PointsAccountResponse;
import pvt.mktech.petcare.points.dto.response.PointsCouponTemplateResponse;
import pvt.mktech.petcare.points.entity.PointsCoupon;
import pvt.mktech.petcare.points.entity.PointsRecord;

import java.util.List;
import pvt.mktech.petcare.points.service.PointsCouponService;
import pvt.mktech.petcare.points.service.PointsService;

/**
 * {@code @description}: 积分系统接口
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Tag(name = "积分管理", description = "积分相关接口")
@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
@Validated
public class PointsController {

    private final PointsService pointsService;
    private final PointsCouponService pointsCouponService;

    @Operation(summary = "查询积分账户")
    @GetMapping("/account")
    public Result<PointsAccountResponse> getAccount() {
        Long userId = UserContext.getUserId();
        return Result.success(pointsService.getAccount(userId));
    }

    @Operation(summary = "分页查询积分流水")
    @PostMapping("/records/page")
    public Result<Page<PointsRecord>> pageRecords(
            @RequestParam(value = "pageNumber", defaultValue = "1") Long pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "20") Long pageSize,
            @RequestBody(required = false) PointsRecordQueryRequest request) {
        Long userId = UserContext.getUserId();
        if (request == null) {
            request = new PointsRecordQueryRequest();
        }
        return Result.success(pointsService.pageRecords(userId, pageNumber, pageSize, request));
    }

    @Operation(summary = "分页查询代金券")
    @PostMapping("/coupons/page")
    public Result<Page<PointsCoupon>> pageCoupons(
            @RequestParam(value = "pageNumber", defaultValue = "1") Long pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "20") Long pageSize,
            @RequestBody(required = false) PointsCouponQueryRequest request) {
        Long userId = UserContext.getUserId();
        if (request == null) {
            request = new PointsCouponQueryRequest();
        }
        return Result.success(pointsCouponService.pageCoupons(userId, pageNumber, pageSize, request));
    }

    @Operation(summary = "领取代金券（充值积分）")
    @PostMapping("/coupon/redeem")
    public Result<Void> redeemCoupon(@RequestParam("couponId") Long couponId) {
        Long userId = UserContext.getUserId();
        pointsCouponService.redeemCoupon(userId, couponId);
        return Result.success();
    }

    @Operation(summary = "抢劵")
    @PostMapping("/coupon/grab")
    public Result<Long> grabCoupon(@RequestParam("templateId") Long templateId) {
        Long userId = UserContext.getUserId();
        Long couponId = pointsCouponService.grabCoupon(userId, templateId);
        return Result.success(couponId);
    }

    @Operation(summary = "查询可供抢券的模板列表")
    @GetMapping("/coupon/templates")
    public Result<List<PointsCouponTemplateResponse>> listActiveTemplates() {
        return Result.success(pointsCouponService.listActiveTemplates());
    }

    @Operation(summary = "消耗积分（AI咨询）")
    @PostMapping("/consume")
    public Result<Void> consume(@RequestBody PointsConsumeRequest request) {
        request.setUserId(UserContext.getUserId());
        pointsService.consume(request);
        return Result.success();
    }
}
