-- 初始化默认标签数据
-- 1-通用标签 2-宠物品种 3-内容类型

-- ========== 通用标签 (type=1) ==========
INSERT INTO tb_label (name, type, icon, color, use_count, is_recommended, status, created_at, updated_at, is_deleted) VALUES
('萌宠', 1, 'icon-pet', '#FF6B6B', 0, 1, 1, NOW(), NOW(), 0),
('护理', 1, 'icon-care', '#4ECDC4', 0, 0, 1, NOW(), NOW(), 0),
('健康', 1, 'icon-health', '#95E1D3', 0, 1, 1, NOW(), NOW(), 0),
('训练', 1, 'icon-train', '#F38181', 0, 0, 1, NOW(), NOW(), 0),
('美食', 1, 'icon-food', '#AA96DA', 0, 0, 1, NOW(), NOW(), 0),
('出行', 1, 'icon-travel', '#FCBAD3', 0, 0, 1, NOW(), NOW(), 0),
('问答', 1, 'icon-qa', '#FFFFD2', 0, 1, 1, NOW(), NOW(), 0),
('经验分享', 1, 'icon-share', '#A8D8EA', 0, 1, 1, NOW(), NOW(), 0);

-- ========== 宠物品种-狗 (type=2) ==========
INSERT INTO tb_label (name, type, icon, color, use_count, is_recommended, status, created_at, updated_at, is_deleted) VALUES
('金毛', 2, 'icon-dog-golden', '#FFD93D', 0, 0, 1, NOW(), NOW(), 0),
('拉布拉多', 2, 'icon-dog-labrador', '#FFD93D', 0, 0, 1, NOW(), NOW(), 0),
('哈士奇', 2, 'icon-dog-husky', '#6BCB77', 0, 0, 1, NOW(), NOW(), 0),
('柯基', 2, 'icon-dog-corgi', '#FF6B6B', 0, 0, 1, NOW(), NOW(), 0),
('泰迪', 2, 'icon-dog-poodle', '#4D96FF', 0, 0, 1, NOW(), NOW(), 0),
('博美', 2, 'icon-dog-pomeranian', '#FFB6B9', 0, 0, 1, NOW(), NOW(), 0),
('比熊', 2, 'icon-dog-bichon', '#FFF5E1', 0, 0, 1, NOW(), NOW(), 0),
('萨摩耶', 2, 'icon-dog-samoyed', '#FFFDF5', 0, 1, 1, NOW(), NOW(), 0),
('柴犬', 2, 'icon-dog-shiba', '#E85A4F', 0, 0, 1, NOW(), NOW(), 0),
('德国牧羊犬', 2, 'icon-dog-german', '#D8A7B1', 0, 0, 1, NOW(), NOW(), 0),
('边牧', 2, 'icon-dog-border', '#1B998B', 0, 0, 1, NOW(), NOW(), 0),
('法斗', 2, 'icon-dog-french', '#F4E04D', 0, 0, 1, NOW(), NOW(), 0);

-- ========== 宠物品种-猫 (type=2) ==========
INSERT INTO tb_label (name, type, icon, color, use_count, is_recommended, status, created_at, updated_at, is_deleted) VALUES
('英短', 2, 'icon-cat-british', '#9B89B3', 0, 0, 1, NOW(), NOW(), 0),
('美短', 2, 'icon-cat-american', '#C4A484', 0, 0, 1, NOW(), NOW(), 0),
('布偶猫', 2, 'icon-cat-ragdoll', '#E8D5E0', 0, 0, 1, NOW(), NOW(), 0),
('暹罗猫', 2, 'icon-cat-siamese', '#B5A8A8', 0, 0, 1, NOW(), NOW(), 0),
('波斯猫', 2, 'icon-cat-persian', '#E8DCCA', 0, 0, 1, NOW(), NOW(), 0),
('加菲猫', 2, 'icon-cat-exotic', '#D4C4A8', 0, 0, 1, NOW(), NOW(), 0),
('缅因猫', 2, 'icon-cat-maine', '#8B7355', 0, 0, 1, NOW(), NOW(), 0),
('橘猫', 2, 'icon-cat-orange', '#FFA500', 0, 0, 1, NOW(), NOW(), 0),
('田园猫', 2, 'icon-cat-tabby', '#D2B48C', 0, 0, 1, NOW(), NOW(), 0),
('斯芬克斯', 2, 'icon-cat-sphynx', '#E0DAC4', 0, 0, 1, NOW(), NOW(), 0);

-- ========== 宠物品种-其他 (type=2) ==========
INSERT INTO tb_label (name, type, icon, color, use_count, is_recommended, status, created_at, updated_at, is_deleted) VALUES
('仓鼠', 2, 'icon-hamster', '#F4A460', 0, 0, 1, NOW(), NOW(), 0),
('兔子', 2, 'icon-rabbit', '#E6E6FA', 0, 0, 1, NOW(), NOW(), 0),
('鸟类', 2, 'icon-bird', '#87CEEB', 0, 0, 1, NOW(), NOW(), 0),
('水族', 2, 'icon-fish', '#00CED1', 0, 0, 1, NOW(), NOW(), 0);

-- ========== 内容类型 (type=3) ==========
-- 对应 Post.postType: 1-好物分享 2-服务推荐 3-地点推荐 4-日常分享 5-活动打卡 6-活动报名
INSERT INTO tb_label (name, type, icon, color, use_count, is_recommended, status, created_at, updated_at, is_deleted) VALUES
('好物分享', 3, 'icon-product', '#FF6B9D', 0, 1, 1, NOW(), NOW(), 0),
('服务推荐', 3, 'icon-service', '#6BCB77', 0, 1, 1, NOW(), NOW(), 0),
('地点推荐', 3, 'icon-location', '#4D96FF', 0, 1, 1, NOW(), NOW(), 0),
('日常分享', 3, 'icon-daily', '#FFD93D', 0, 1, 1, NOW(), NOW(), 0),
('活动打卡', 3, 'icon-checkin', '#FF6B6B', 0, 0, 1, NOW(), NOW(), 0),
('活动报名', 3, 'icon-join', '#95E1D3', 0, 0, 1, NOW(), NOW(), 0);
