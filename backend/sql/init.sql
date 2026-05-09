-- ============================================================================
-- 票务系统 (Ticket System) - 数据库初始化脚本
-- 目标数据库: ticket_system
-- 字符集: utf8mb4 / utf8mb4_unicode_ci
-- MySQL版本: 8.0+
-- ============================================================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS `ticket_system`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `ticket_system`;

-- ============================================================================
-- 1. 用户表 (sys_user)
--    存储系统用户信息，包括管理员和普通用户
-- ============================================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `username`    VARCHAR(50)  NOT NULL COMMENT 'Username',
    `password`    VARCHAR(100) NOT NULL COMMENT 'Password (当前使用明文，建议生产环境BCrypt加密)',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT 'Phone Number',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT 'Email',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT 'Nickname',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT 'Avatar URL',
    `role`        VARCHAR(20)  DEFAULT 'user' COMMENT 'Role: user=普通用户, admin=管理员',
    `status`      TINYINT      DEFAULT '1' COMMENT 'Status: 0=禁用, 1=启用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `username` (`username`),
    KEY `idx_username` (`username`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User Table (用户表)';

-- ============================================================================
-- 2. 演出表 (biz_show)
--    存储演出/活动的基本信息，如名称、分类、场馆、时间等
-- ============================================================================
DROP TABLE IF EXISTS `biz_show`;
CREATE TABLE `biz_show` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `name`        VARCHAR(200) NOT NULL COMMENT 'Show Name (演出名称)',
    `poster`      VARCHAR(500) DEFAULT NULL COMMENT 'Poster URL (海报URL)',
    `description` TEXT COMMENT 'Show Description (演出描述)',
    `venue`       VARCHAR(200) DEFAULT NULL COMMENT 'Venue (演出场馆)',
    `city`        VARCHAR(50)  DEFAULT NULL COMMENT 'City (城市)',
    `category`    VARCHAR(50)  DEFAULT NULL COMMENT 'Category (分类: 演唱会/话剧/体育/舞蹈/儿童亲子/展览)',
    `start_time`  DATETIME     DEFAULT NULL COMMENT 'Start Time (演出开始时间)',
    `end_time`    DATETIME     DEFAULT NULL COMMENT 'End Time (演出结束时间)',
    `status`      TINYINT      DEFAULT '1' COMMENT 'Status: 0=下架(Offline), 1=上架(OnSale), 2=进行中(Ongoing)',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Show Table (演出表)';

-- ============================================================================
-- 3. 票档表 (biz_ticket)
--    存储每个演出的不同票档，如VIP票、甲票、乙票等
--    available_stock 通过乐观锁（WHERE available_stock >= quantity）防止超卖
-- ============================================================================
DROP TABLE IF EXISTS `biz_ticket`;
CREATE TABLE `biz_ticket` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `show_id`         BIGINT       NOT NULL COMMENT 'Show ID (关联biz_show)',
    `name`            VARCHAR(100) NOT NULL COMMENT 'Ticket Category Name (票档名称: VIP票/甲票/乙票等)',
    `price`           DECIMAL(10,2) NOT NULL COMMENT 'Price (单价)',
    `total_stock`     INT          NOT NULL COMMENT 'Total Stock (总库存)',
    `available_stock` INT          NOT NULL COMMENT 'Available Stock (可用库存，下单时实时扣减)',
    `seat_range`      VARCHAR(100) DEFAULT NULL COMMENT 'Seat Range (座位范围描述)',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    KEY `idx_show_id` (`show_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Ticket Table (票档表)';

-- ============================================================================
-- 4. 座位表 (biz_seat)
--    存储具体座位信息，用于选座购票场景
-- ============================================================================
DROP TABLE IF EXISTS `biz_seat`;
CREATE TABLE `biz_seat` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `ticket_id`   BIGINT      NOT NULL COMMENT 'Ticket Category ID (关联biz_ticket)',
    `seat_no`     VARCHAR(20) NOT NULL COMMENT 'Seat Number (座位号，如 VIP-A01-01)',
    `status`      TINYINT     DEFAULT '0' COMMENT 'Status: 0=可选(Available), 1=已锁定(Locked), 2=已售(Sold)',
    `order_id`    BIGINT      DEFAULT NULL COMMENT 'Order ID (关联biz_order)',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_status` (`status`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Seat Table (座位表)';

-- ============================================================================
-- 5. 秒杀场次表 (biz_seckill_session)
--    存储秒杀活动的场次信息
--    注意：
--    - 秒杀期间的库存存储在 Redis 中（key: seckill:stock:{sessionId}）
--    - 数据库库存通过 MQ 异步与 Redis 保持一致（最终一致性）
--    - deductStock 使用乐观锁（WHERE stock >= quantity）
-- ============================================================================
DROP TABLE IF EXISTS `biz_seckill_session`;
CREATE TABLE `biz_seckill_session` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `show_id`       BIGINT       NOT NULL COMMENT 'Show ID (关联biz_show)',
    `ticket_id`     BIGINT       NOT NULL COMMENT 'Ticket Category ID (关联biz_ticket)',
    `name`          VARCHAR(255) DEFAULT NULL COMMENT '场次名称',
    `seckill_price` DECIMAL(10,2) DEFAULT NULL COMMENT 'Seckill Price (秒杀价格)',
    `start_time`    DATETIME     NOT NULL COMMENT 'Seckill Start Time (秒杀开始时间)',
    `end_time`      DATETIME     NOT NULL COMMENT 'Seckill End Time (秒杀结束时间)',
    `stock`         INT          NOT NULL COMMENT 'Seckill Stock (秒杀库存)',
    `status`        TINYINT      DEFAULT '0' COMMENT 'Status: 0=未开始, 1=进行中, 2=已结束',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    KEY `idx_show_id` (`show_id`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Seckill Session Table (秒杀场次表)';

-- ============================================================================
-- 6. 订单表 (biz_order)
--    存储用户订单信息
--    订单状态流转:
--      待支付(0) ───支付成功──→ 已支付(1) ───完成──→ 已完成(4)
--         │                                                ▲
--         ├──取消/超时──→ 已取消(2)                        │
--         └──────────────────── 退款 ──────────────────────┘
--                              已支付(1) ─→ 已退款(3)
-- ============================================================================
DROP TABLE IF EXISTS `biz_order`;
CREATE TABLE `biz_order` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `order_no`     VARCHAR(50)   NOT NULL COMMENT 'Order Number (订单号，格式: ORD+时间戳+6位随机数)',
    `user_id`      BIGINT        NOT NULL COMMENT 'User ID (用户ID)',
    `show_id`      BIGINT        NOT NULL COMMENT 'Show ID (演出ID)',
    `ticket_id`    BIGINT        NOT NULL COMMENT 'Ticket Category ID (票档ID)',
    `seat_id`      BIGINT        DEFAULT NULL COMMENT 'Seat ID (座位ID，选座时使用)',
    `quantity`     INT           DEFAULT '1' COMMENT 'Quantity (购买数量)',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT 'Total Amount (订单总金额)',
    `status`       TINYINT       DEFAULT '0' COMMENT 'Status: 0=待支付(Pending), 1=已支付(Paid), 2=已取消(Cancelled), 3=已退款(Refunded), 4=已完成(Completed)',
    `pay_time`     DATETIME      DEFAULT NULL COMMENT 'Payment Time (支付时间)',
    `expire_time`  DATETIME      DEFAULT NULL COMMENT 'Order Expiration Time (过期时间，创建后15分钟)',
    `create_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    `update_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    `is_seckill`   TINYINT(1)    DEFAULT '0' COMMENT '是否秒杀订单: 0-否(普通订单), 1-是(秒杀订单)',
    `session_id`   BIGINT        DEFAULT NULL COMMENT '秒杀场次ID(普通订单为NULL，关联biz_seckill_session)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `order_no` (`order_no`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_show_id` (`show_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Order Table (订单表)';


-- ============================================================================
-- XXX  初始化数据 (Init Data)
-- ============================================================================

-- ============================================================================
-- 3.1 用户数据
--     注意：当前项目使用明文密码！
--     账号: admin / password: admin123 (管理员角色)
--     账号: test  / password: admin123 (管理员角色)
--     账号: user1 / password: admin123 (普通用户)
-- ============================================================================
INSERT INTO `sys_user` (`id`, `username`, `password`, `phone`, `email`, `nickname`, `avatar`, `role`, `status`, `create_time`, `update_time`) VALUES
(1, 'admin', 'admin123', '13800138000', 'admin@ticket.com', '管理员', 'https://api.dicebear.com/9.x/adventurer/svg?seed=gpc', 'admin', 1, '2026-04-21 20:16:08', NULL),
(2, 'test', 'admin123', '13800138001', 'test@ticket.com', 'Test User', NULL, 'admin', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),
(3, 'user1', 'admin123', '13800138002', 'user1@ticket.com', 'User One', NULL, 'user', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08');

-- ============================================================================
-- 3.2 演出数据 (13条)
--     分类: 演唱会、话剧、体育、舞蹈、儿童亲子、展览
-- ============================================================================
INSERT INTO `biz_show` (`id`, `name`, `poster`, `description`, `venue`, `city`, `category`, `start_time`, `end_time`, `status`, `create_time`, `update_time`) VALUES
(1, '周杰伦2024世界巡回演唱会北京站',
    'https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?w=700',
    '周杰伦2024世界巡回演唱会北京站，时隔三年再度来京，用最顶级的制作、最震撼的音响效果，为北京的歌迷带来一场视听盛宴！',
    '北京鸟巢体育场', '北京', '演唱会',
    '2024-12-01 19:30:00', '2024-12-01 22:00:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(2, '开心麻花爆笑喜剧《乌龙山伯爵》',
    'https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?w=700',
    '开心麻花经典喜剧《乌龙山伯爵》，一个无工作、无房子、无车子的"三无"青年谢蟹，在三十岁生日这天忽然得到一笔巨额遗产，又莫名其妙被误认为劫匪，被迫逃亡...',
    '北京地质礼堂', '北京', '话剧',
    '2024-11-15 19:30:00', '2024-11-15 21:30:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(3, 'CBA季后赛广东VS辽宁',
    'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=700',
    '2023-2024赛季CBA季后赛半决赛，广东华南虎对阵辽宁本钢，强强对话，谁将晋级决赛？',
    '广州体育馆', '广州', '体育',
    '2024-11-20 19:35:00', '2024-11-20 21:35:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(4, '薛之谦"天外来物"巡回演唱会',
    'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=700',
    '薛之谦2024巡回演唱会，以"音乐+喜剧"的创新形式呈现，融合各种音乐风格，打造一场前所未有的音乐体验。',
    '上海梅赛德斯奔驰文化中心', '上海', '演唱会',
    '2024-12-15 19:30:00', '2024-12-15 22:00:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(5, '周杰伦2024台北站',
    'https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?w=700',
    '周杰伦2024世界巡回演唱会台北站，回到最初梦想开始的地方，与台北的歌迷共度难忘音乐之夜。',
    '台北小巨蛋', '台北', '演唱会',
    '2024-10-25 19:30:00', '2024-10-25 22:00:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(6, '儿童剧《冰雪奇缘》',
    'https://picsum.photos/700/700?random=20241220',
    '经典动画《冰雪奇缘》改编舞台剧，迪士尼正版授权，绚丽舞台效果，带孩子进入梦幻的冰雪世界！',
    '深圳保利剧院', '深圳', '儿童亲子',
    '2024-12-20 10:30:00', '2024-12-20 12:00:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(7, '五月天2024诺亚方舟演唱会',
    'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=700',
    '五月天2024世界巡回演唱会，乘坐诺亚方舟，穿越音乐海洋，与你相约最好的一天！',
    '香港红磡体育馆', '香港', '演唱会',
    '2024-11-30 19:30:00', '2024-11-30 22:30:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(8, '芭蕾舞剧《天鹅湖》',
    'https://images.unsplash.com/photo-1518834107812-67b0b7c58434?w=700',
    '俄罗斯皇家芭蕾舞团经典呈现，柴可夫斯基传世名作，感受古典芭蕾的极致优雅。',
    '北京国家大剧院', '北京', '舞蹈',
    '2024-10-10 19:30:00', '2024-10-10 21:30:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(9, '「星河巡回」演唱会·上海站',
    'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?w=800&h=1000&fit=crop',
    '华语流行演唱会，舞美与音响全面升级，经典金曲连唱。',
    '梅赛德斯-奔驰文化中心', '上海', '演唱会',
    '2026-07-18 19:30:00', '2026-07-18 22:00:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(10, '《暗恋桃花源》经典话剧',
    'https://images.unsplash.com/photo-1503095396549-807759245b35?w=800&h=1000&fit=crop',
    '赖声川代表作，悲喜交错的城市寓言，全国巡演驻场。',
    '国家大剧院·戏剧场', '北京', '话剧',
    '2026-06-06 19:30:00', '2026-06-06 21:30:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(11, '2026CBA全明星周末',
    'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800&h=1000&fit=crop',
    '南北区球星对抗、扣篮大赛与三分大赛，一票看全。',
    '广州体育馆', '广州', '体育',
    '2026-05-01 18:00:00', '2026-05-01 22:00:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(12, '「印象派与光影」沉浸式艺术展',
    'https://picsum.photos/800/1000?random=20260420',
    '莫奈、雷诺阿等大师复刻与数字光影互动，适合打卡与亲子共览。',
    '西岸美术馆', '上海', '展览',
    '2026-04-20 10:00:00', '2026-08-31 18:00:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(13, '《木偶奇遇记》亲子互动舞台剧',
    'https://images.unsplash.com/photo-1513885535751-8b9238bd345a?w=800&h=1000&fit=crop',
    '经典童话改编，全场互动与小小工作坊，建议3岁以上家庭观演。',
    '上海儿童艺术剧场', '上海', '儿童亲子',
    '2026-06-01 10:30:00', '2026-06-01 12:00:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08');

-- ============================================================================
-- 3.3 票档数据 (42条)
--     每场演出2~5个票档，价格从低到高排列
-- ============================================================================
INSERT INTO `biz_ticket` (`id`, `show_id`, `name`, `price`, `total_stock`, `available_stock`, `seat_range`) VALUES
-- 演出1: 周杰伦北京站
(1, 1, 'VIP票', 1999.00, 500, 500, '内场VIP区'),
(2, 1, '甲票', 1299.00, 1000, 1000, '内场A区'),
(3, 1, '乙票',  799.00, 2000, 2000, '看台一层'),
(4, 1, '丙票',  499.00, 3000, 3000, '看台二层'),
-- 演出2: 乌龙山伯爵
(5, 2, 'VIP票',  680.00, 100, 100, '池座1-5排'),
(6, 2, '甲票',  480.00, 200, 200, '池座6-15排'),
(7, 2, '乙票',  280.00, 300, 300, '楼座'),
-- 演出3: CBA季后赛
(8, 3, '内场VIP', 1280.00, 200, 200, '内场前排'),
(9, 3, '甲票',   880.00, 500, 500, '内场'),
(10, 3, '乙票',  480.00, 1000, 1000, '看台'),
-- 演出4: 薛之谦上海站
(11, 4, 'VIP票', 1680.00, 300, 300, '内场VIP区'),
(12, 4, '甲票', 1080.00, 800, 800, '内场A区'),
(13, 4, '乙票',  680.00, 1500, 1500, '看台'),
-- 演出5: 周杰伦台北站
(14, 5, '特A票', 2888.00, 200, 200, '特A区：1-20排'),
(15, 5, '甲票',  1888.00, 800, 800, 'A区：21-50排'),
(16, 5, '乙票',  1288.00, 1500, 1500, 'B区'),
(17, 5, '丙票',   888.00, 2500, 2500, 'C区'),
-- 演出6: 冰雪奇缘
(18, 6, 'VIP亲子票', 380.00, 100, 100, 'VIP区（一大一小）'),
(19, 6, '甲票',      280.00, 300, 300, 'A区'),
(20, 6, '乙票',      180.00, 400, 400, 'B区'),
(21, 6, '学生票',    100.00, 200, 200, '学生区'),
-- 演出7: 五月天香港站
(22, 7, 'VIP票', 1688.00, 1000, 1000, '内场VIP'),
(23, 7, '甲票',  1088.00, 1800, 1800, '内场A'),
(24, 7, '乙票',   688.00, 2200, 2200, '内场B'),
(25, 7, '看台票', 388.00, 1500, 1500, '看台'),
-- 演出8: 天鹅湖
(26, 8, 'VIP票',   980.00, 150, 150, 'VIP区前排'),
(27, 8, '甲票',    680.00, 300, 300, 'A区'),
(28, 8, '乙票',    480.00, 400, 400, 'B区'),
(29, 8, '学生票',  280.00, 200, 200, '学生区'),
-- 演出9: 星河巡回
(30, 9, '内场VIP', 1288.00,  800,  800, '内场前区'),
(31, 9, '看台A档',  688.00, 2000, 2000, '看台一层'),
(32, 9, '看台B档',  388.00, 3500, 3500, '看台二层'),
-- 演出10: 暗恋桃花源
(33, 10, '一等座', 580.00, 400, 400, '池座中区'),
(34, 10, '二等座', 380.00, 600, 600, '池座两侧'),
(35, 10, '学生票', 180.00, 120, 120, '学生活动区'),
-- 演出11: CBA全明星
(36, 11, '内场通票', 880.00, 3000, 3000, '内场全区域'),
(37, 11, '看台票',   280.00, 8000, 8000, '环形看台'),
-- 演出12: 印象派光影展
(38, 12, '早鸟通票',      128.00, 5000, 5000, '全展期通用'),
(39, 12, '亲子套票（一大一小）', 198.00, 800, 800, '含儿童导览册'),
-- 演出13: 木偶奇遇记
(40, 13, '亲子座（含互动）',  320.00, 260, 260, '前排互动区'),
(41, 13, '家庭套票（三人）',  680.00, 120, 120, '中区连座'),
(42, 13, '普通座',           180.00, 400, 400, '后排区域');

-- ============================================================================
-- 3.4 座位数据 (15条)
--     仅周杰伦北京站VIP票档有示例座位数据
-- ============================================================================
INSERT INTO `biz_seat` (`id`, `ticket_id`, `seat_no`, `status`, `order_id`, `create_time`, `update_time`) VALUES
(1,  1, 'VIP-A01-01', 0, NULL, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),
(2,  1, 'VIP-A01-02', 0, NULL, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),
(3,  1, 'VIP-A01-03', 0, NULL, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),
(4,  1, 'VIP-A01-04', 0, NULL, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),
(5,  1, 'VIP-A01-05', 0, NULL, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),
(6,  1, 'VIP-A02-01', 0, NULL, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),
(7,  1, 'VIP-A02-02', 0, NULL, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),
(8,  1, 'VIP-A02-03', 0, NULL, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),
(9,  1, 'VIP-A02-04', 0, NULL, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),
(10, 1, 'VIP-A02-05', 0, NULL, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),
(11, 1, 'VIP-A03-01', 0, NULL, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),
(12, 1, 'VIP-A03-02', 0, NULL, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),
(13, 1, 'VIP-A03-03', 0, NULL, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),
(14, 1, 'VIP-A03-04', 0, NULL, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),
(15, 1, 'VIP-A03-05', 0, NULL, '2026-04-21 20:16:08', '2026-04-21 20:16:08');

-- ============================================================================
-- 3.5 秒杀场次数据 (6条)
--     注意：start_time 和 end_time 为秒杀活动时间范围
--     当前数据设为2030年，表示未开始的秒杀场次
--     如需测试，请将start_time改为过去时间
-- ============================================================================
INSERT INTO `biz_seckill_session` (`id`, `show_id`, `ticket_id`, `name`, `seckill_price`, `start_time`, `end_time`, `stock`, `status`, `create_time`, `update_time`) VALUES
(1, 1, 1, '周杰伦北京站-VIP秒杀', 999.00,  '2026-04-21 20:16:08', '2027-04-30 22:16:08', 100, 1, '2026-04-21 20:16:08', '2026-05-07 23:13:36'),
(2, 1, 2, '周杰伦北京站-甲票秒杀', 699.00,  '2026-04-21 20:16:08', '2027-04-30 22:16:08', 200, 1, '2026-04-21 20:16:08', '2026-05-07 23:13:41'),
(3, 1, 3, '周杰伦北京站-乙票秒杀', 399.00,  '2026-04-23 20:16:08', '2027-04-30 22:16:08', 300, 1, '2026-04-21 20:16:08', '2026-05-07 23:13:44'),
(4, 4, 11, '薛之谦北京站-VIP秒杀', 899.00, '2026-04-24 20:16:08', '2027-04-30 22:16:08', 80,  1, '2026-04-21 20:16:08', NULL),
(5, 5, 14, '周杰伦台北站-特A秒杀', 1888.00,'2026-04-26 20:16:08', '2027-04-30 22:16:08', 50,  1, '2026-04-21 20:16:08', '2026-05-07 23:12:59'),
(6, 5, 15, '周杰伦台北站-甲票秒杀', 988.00, '2026-04-26 20:16:08', '2027-04-30 22:16:08', 100, 1, '2026-04-21 20:16:08', NULL);

-- ============================================================================
-- 3.6 订单数据（当前为空）
--     订单通过系统下单接口动态生成，此处不预置测试数据
-- ============================================================================


-- ============================================================================
-- XXX  重置自增主键（确保新插入数据的ID按预期增长）
-- ============================================================================
-- 如果不需要重置主键起始值，注释掉以下语句
-- ALTER TABLE sys_user            AUTO_INCREMENT = 4;
-- ALTER TABLE biz_show            AUTO_INCREMENT = 14;
-- ALTER TABLE biz_ticket          AUTO_INCREMENT = 43;
-- ALTER TABLE biz_seat            AUTO_INCREMENT = 16;
-- ALTER TABLE biz_seckill_session AUTO_INCREMENT = 7;
-- ALTER TABLE biz_order           AUTO_INCREMENT = 1;
