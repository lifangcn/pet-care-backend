package pvt.mktech.petcare.points.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.points.dto.response.PointsLevelResponse;
import pvt.mktech.petcare.points.service.PointsLevelService;

import java.util.Arrays;
import java.util.List;

/**
 * {@code @description}: 积分等级服务实现
 * {@code @date}: 2025/02/06
 *
 * @author Michael
 */
@Service
@RequiredArgsConstructor
public class PointsLevelServiceImpl implements PointsLevelService {

    private static final int[][] LEVEL_RULES = {
            {0, 99},
            {100, 499},
            {500, 1499},
            {1500, 3499},
            {3500, 6999},
            {7000, 14999},
            {15000, 29999},
            {30000, 59999},
            {60000, 119999},
            {120000, Integer.MAX_VALUE}
    };

    private static final String[] LEVEL_TITLES = {
            "萌新铲屎官",
            "入门铲屎官",
            "熟练铲屎官",
            "资深铲屎官",
            "专家铲屎官",
            "宠物达人",
            "圈内红人",
            "宠物大师",
            "社区领袖",
            "宠物宗师"
    };

    private static final List<String>[] LEVEL_BENEFITS = new List[]{
            List.of("基础功能"),
            List.of("基础功能", "每日积分上限+50"),
            List.of("基础功能", "每日积分上限+50", "内容曝光权重+10%"),
            List.of("基础功能", "每日积分上限+50", "内容曝光权重+10%", "专属标识", "积分获取+10%"),
            List.of("基础功能", "每日积分上限+50", "内容曝光权重+10%", "专属标识", "积分获取+10%", "AI咨询8折", "优先客服"),
            List.of("基础功能", "每日积分上限+50", "内容曝光权重+25%", "专属标识", "积分获取+10%", "AI咨询8折", "优先客服"),
            List.of("基础功能", "每日积分上限+50", "内容曝光权重+25%", "专属标识", "积分获取+10%", "AI咨询8折", "优先客服", "官方活动优先参与权"),
            List.of("基础功能", "每日积分上限+50", "内容曝光权重+25%", "专属标识", "积分获取+10%", "AI咨询8折", "优先客服", "官方活动优先参与权", "受邀参与产品内测"),
            List.of("基础功能", "每日积分上限+50", "内容曝光权重+25%", "专属标识", "积分获取+10%", "AI咨询8折", "优先客服", "官方活动优先参与权", "受邀参与产品内测", "专属客服通道"),
            List.of("基础功能", "每日积分上限+50", "内容曝光权重+25%", "专属标识", "积分获取+10%", "AI咨询8折", "优先客服", "官方活动优先参与权", "受邀参与产品内测", "专属客服通道", "所有权益最大化", "定制化服务")
    };

    @Override
    public Integer calculateLevel(Integer totalPoints) {
        for (int i = 0; i < LEVEL_RULES.length; i++) {
            if (totalPoints >= LEVEL_RULES[i][0] && totalPoints <= LEVEL_RULES[i][1]) {
                return i + 1;
            }
        }
        return 1;
    }

    @Override
    public PointsLevelResponse getLevelInfo(Integer level) {
        if (level < 1 || level > 10) {
            level = 1;
        }

        PointsLevelResponse response = new PointsLevelResponse();
        response.setLevel(level);
        response.setTitle(LEVEL_TITLES[level - 1]);
        response.setRequiredPoints(LEVEL_RULES[level - 1][0]);
        response.setBenefits(LEVEL_BENEFITS[level - 1]);

        // 下一等级所需积分
        if (level < 10) {
            response.setNextLevelPoints(LEVEL_RULES[level][0]);
        } else {
            response.setNextLevelPoints(LEVEL_RULES[9][0]);
        }

        return response;
    }
}
