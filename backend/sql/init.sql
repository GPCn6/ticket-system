-- ============================================================================
-- 绁ㄥ姟绯荤粺 (Ticket System) - 鏁版嵁搴撳垵濮嬪寲鑴氭湰
-- 鐩爣鏁版嵁搴? ticket_system
-- 瀛楃闆? utf8mb4 / utf8mb4_unicode_ci
-- MySQL鐗堟湰: 8.0+
-- ============================================================================

-- 鍒涘缓鏁版嵁搴擄紙濡備笉瀛樺湪锛?CREATE DATABASE IF NOT EXISTS `ticket_system`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `ticket_system`;

-- ============================================================================
-- 1. 鐢ㄦ埛琛?(sys_user)
--    瀛樺偍绯荤粺鐢ㄦ埛淇℃伅锛屽寘鎷鐞嗗憳鍜屾櫘閫氱敤鎴?-- ============================================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `username`    VARCHAR(50)  NOT NULL COMMENT 'Username',
    `password`    VARCHAR(100) NOT NULL COMMENT 'Password (褰撳墠浣跨敤鏄庢枃锛屽缓璁敓浜х幆澧傿Crypt鍔犲瘑)',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT 'Phone Number',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT 'Email',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT 'Nickname',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT 'Avatar URL',
    `role`        VARCHAR(20)  DEFAULT 'user' COMMENT 'Role: user=鏅€氱敤鎴? admin=绠＄悊鍛?,
    `status`      TINYINT      DEFAULT '1' COMMENT 'Status: 0=绂佺敤, 1=鍚敤',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `username` (`username`),
    KEY `idx_username` (`username`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User Table (鐢ㄦ埛琛?';

-- ============================================================================
-- 2. 婕斿嚭琛?(biz_show)
--    瀛樺偍婕斿嚭/娲诲姩鐨勫熀鏈俊鎭紝濡傚悕绉般€佸垎绫汇€佸満棣嗐€佹椂闂寸瓑
-- ============================================================================
DROP TABLE IF EXISTS `biz_show`;
CREATE TABLE `biz_show` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `name`        VARCHAR(200) NOT NULL COMMENT 'Show Name (婕斿嚭鍚嶇О)',
    `poster`      VARCHAR(500) DEFAULT NULL COMMENT 'Poster URL (娴锋姤URL)',
    `description` TEXT COMMENT 'Show Description (婕斿嚭鎻忚堪)',
    `venue`       VARCHAR(200) DEFAULT NULL COMMENT 'Venue (婕斿嚭鍦洪)',
    `city`        VARCHAR(50)  DEFAULT NULL COMMENT 'City (鍩庡競)',
    `category`    VARCHAR(50)  DEFAULT NULL COMMENT 'Category (鍒嗙被: 婕斿敱浼?璇濆墽/浣撹偛/鑸炶箞/鍎跨浜插瓙/灞曡)',
    `start_time`  DATETIME     DEFAULT NULL COMMENT 'Start Time (婕斿嚭寮€濮嬫椂闂?',
    `end_time`    DATETIME     DEFAULT NULL COMMENT 'End Time (婕斿嚭缁撴潫鏃堕棿)',
    `status`      TINYINT      DEFAULT '1' COMMENT 'Status: 0=涓嬫灦(Offline), 1=涓婃灦(OnSale), 2=杩涜涓?Ongoing)',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Show Table (婕斿嚭琛?';

-- ============================================================================
-- 3. 绁ㄦ。琛?(biz_ticket)
--    瀛樺偍姣忎釜婕斿嚭鐨勪笉鍚岀エ妗ｏ紝濡俈IP绁ㄣ€佺敳绁ㄣ€佷箼绁ㄧ瓑
--    available_stock 閫氳繃涔愯閿侊紙WHERE available_stock >= quantity锛夐槻姝㈣秴鍗?-- ============================================================================
DROP TABLE IF EXISTS `biz_ticket`;
CREATE TABLE `biz_ticket` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `show_id`         BIGINT       NOT NULL COMMENT 'Show ID (鍏宠仈biz_show)',
    `name`            VARCHAR(100) NOT NULL COMMENT 'Ticket Category Name (绁ㄦ。鍚嶇О: VIP绁?鐢茬エ/涔欑エ绛?',
    `price`           DECIMAL(10,2) NOT NULL COMMENT 'Price (鍗曚环)',
    `total_stock`     INT          NOT NULL COMMENT 'Total Stock (鎬诲簱瀛?',
    `available_stock` INT          NOT NULL COMMENT 'Available Stock (鍙敤搴撳瓨锛屼笅鍗曟椂瀹炴椂鎵ｅ噺)',
    `seat_range`      VARCHAR(100) DEFAULT NULL COMMENT 'Seat Range (搴т綅鑼冨洿鎻忚堪)',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    KEY `idx_show_id` (`show_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Ticket Table (绁ㄦ。琛?';

-- ============================================================================
-- 4. 搴т綅琛?(biz_seat)
--    瀛樺偍鍏蜂綋搴т綅淇℃伅锛岀敤浜庨€夊骇璐エ鍦烘櫙
-- ============================================================================
DROP TABLE IF EXISTS `biz_seat`;
CREATE TABLE `biz_seat` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `ticket_id`   BIGINT      NOT NULL COMMENT 'Ticket Category ID (鍏宠仈biz_ticket)',
    `seat_no`     VARCHAR(20) NOT NULL COMMENT 'Seat Number (搴т綅鍙凤紝濡?VIP-A01-01)',
    `status`      TINYINT     DEFAULT '0' COMMENT 'Status: 0=鍙€?Available), 1=宸查攣瀹?Locked), 2=宸插敭(Sold)',
    `order_id`    BIGINT      DEFAULT NULL COMMENT 'Order ID (鍏宠仈biz_order)',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_status` (`status`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Seat Table (搴т綅琛?';

-- ============================================================================
-- 5. 绉掓潃鍦烘琛?(biz_seckill_session)
--    瀛樺偍绉掓潃娲诲姩鐨勫満娆′俊鎭?--    娉ㄦ剰锛?--    - 绉掓潃鏈熼棿鐨勫簱瀛樺瓨鍌ㄥ湪 Redis 涓紙key: seckill:stock:{sessionId}锛?--    - 鏁版嵁搴撳簱瀛橀€氳繃 MQ 寮傛涓?Redis 淇濇寔涓€鑷达紙鏈€缁堜竴鑷存€э級
--    - deductStock 浣跨敤涔愯閿侊紙WHERE stock >= quantity锛?-- ============================================================================
DROP TABLE IF EXISTS `biz_seckill_session`;
CREATE TABLE `biz_seckill_session` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `show_id`       BIGINT       NOT NULL COMMENT 'Show ID (鍏宠仈biz_show)',
    `ticket_id`     BIGINT       NOT NULL COMMENT 'Ticket Category ID (鍏宠仈biz_ticket)',
    `name`          VARCHAR(255) DEFAULT NULL COMMENT '鍦烘鍚嶇О',
    `seckill_price` DECIMAL(10,2) DEFAULT NULL COMMENT 'Seckill Price (绉掓潃浠锋牸)',
    `start_time`    DATETIME     NOT NULL COMMENT 'Seckill Start Time (绉掓潃寮€濮嬫椂闂?',
    `end_time`      DATETIME     NOT NULL COMMENT 'Seckill End Time (绉掓潃缁撴潫鏃堕棿)',
    `stock`         INT          NOT NULL COMMENT 'Seckill Stock (绉掓潃搴撳瓨)',
    `status`        TINYINT      DEFAULT '0' COMMENT 'Status: 0=鏈紑濮? 1=杩涜涓? 2=宸茬粨鏉?,
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    PRIMARY KEY (`id`),
    KEY `idx_show_id` (`show_id`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Seckill Session Table (绉掓潃鍦烘琛?';

-- ============================================================================
-- 6. 璁㈠崟琛?(biz_order)
--    瀛樺偍鐢ㄦ埛璁㈠崟淇℃伅
--    璁㈠崟鐘舵€佹祦杞?
--      寰呮敮浠?0) 鈹€鈹€鈹€鏀粯鎴愬姛鈹€鈹€鈫?宸叉敮浠?1) 鈹€鈹€鈹€瀹屾垚鈹€鈹€鈫?宸插畬鎴?4)
--         鈹?                                               鈻?--         鈹溾攢鈹€鍙栨秷/瓒呮椂鈹€鈹€鈫?宸插彇娑?2)                        鈹?--         鈹斺攢鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€ 閫€娆?鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹?--                              宸叉敮浠?1) 鈹€鈫?宸查€€娆?3)
-- ============================================================================
DROP TABLE IF EXISTS `biz_order`;
CREATE TABLE `biz_order` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    `order_no`     VARCHAR(50)   NOT NULL COMMENT 'Order Number (璁㈠崟鍙凤紝鏍煎紡: ORD+鏃堕棿鎴?6浣嶉殢鏈烘暟)',
    `user_id`      BIGINT        NOT NULL COMMENT 'User ID (鐢ㄦ埛ID)',
    `show_id`      BIGINT        NOT NULL COMMENT 'Show ID (婕斿嚭ID)',
    `ticket_id`    BIGINT        NOT NULL COMMENT 'Ticket Category ID (绁ㄦ。ID)',
    `seat_id`      BIGINT        DEFAULT NULL COMMENT 'Seat ID (搴т綅ID锛岄€夊骇鏃朵娇鐢?',
    `quantity`     INT           DEFAULT '1' COMMENT 'Quantity (璐拱鏁伴噺)',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT 'Total Amount (璁㈠崟鎬婚噾棰?',
    `status`       TINYINT       DEFAULT '0' COMMENT 'Status: 0=寰呮敮浠?Pending), 1=宸叉敮浠?Paid), 2=宸插彇娑?Cancelled), 3=宸查€€娆?Refunded), 4=宸插畬鎴?Completed)',
    `pay_time`     DATETIME      DEFAULT NULL COMMENT 'Payment Time (鏀粯鏃堕棿)',
    `expire_time`  DATETIME      DEFAULT NULL COMMENT 'Order Expiration Time (杩囨湡鏃堕棿锛屽垱寤哄悗15鍒嗛挓)',
    `create_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    `update_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    `is_seckill`   TINYINT(1)    DEFAULT '0' COMMENT '鏄惁绉掓潃璁㈠崟: 0-鍚?鏅€氳鍗?, 1-鏄?绉掓潃璁㈠崟)',
    `session_id`   BIGINT        DEFAULT NULL COMMENT '绉掓潃鍦烘ID(鏅€氳鍗曚负NULL锛屽叧鑱攂iz_seckill_session)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `order_no` (`order_no`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_show_id` (`show_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Order Table (璁㈠崟琛?';


-- ============================================================================
-- XXX  鍒濆鍖栨暟鎹?(Init Data)
-- ============================================================================

-- ============================================================================
-- 3.1 鐢ㄦ埛鏁版嵁
--     娉ㄦ剰锛氬綋鍓嶉」鐩娇鐢ㄦ槑鏂囧瘑鐮侊紒
--     璐﹀彿: admin / password: admin123 (绠＄悊鍛樿鑹?
--     璐﹀彿: test  / password: admin123 (绠＄悊鍛樿鑹?
--     璐﹀彿: user1 / password: admin123 (鏅€氱敤鎴?
-- ============================================================================
INSERT INTO `sys_user` (`id`, `username`, `password`, `phone`, `email`, `nickname`, `avatar`, `role`, `status`, `create_time`, `update_time`) VALUES
(1, 'admin', '$2a$10$QZxT4t7rPg..WPMk7oJZ3eMF/sKUj/3mXqdPaPwftK2d0LMSjLENm', '13800138000', 'admin@ticket.com', '绠＄悊鍛?, 'https://api.dicebear.com/9.x/adventurer/svg?seed=gpc', 'admin', 1, '2026-04-21 20:16:08', NULL),
(2, 'test', '$2a$10$QZxT4t7rPg..WPMk7oJZ3eMF/sKUj/3mXqdPaPwftK2d0LMSjLENm', '13800138001', 'test@ticket.com', 'Test User', NULL, 'admin', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),
(3, 'user1', '$2a$10$QZxT4t7rPg..WPMk7oJZ3eMF/sKUj/3mXqdPaPwftK2d0LMSjLENm', '13800138002', 'user1@ticket.com', 'User One', NULL, 'user', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08');

-- ============================================================================
-- 3.2 婕斿嚭鏁版嵁 (13鏉?
--     鍒嗙被: 婕斿敱浼氥€佽瘽鍓с€佷綋鑲层€佽垶韫堛€佸効绔ヤ翰瀛愩€佸睍瑙?-- ============================================================================
INSERT INTO `biz_show` (`id`, `name`, `poster`, `description`, `venue`, `city`, `category`, `start_time`, `end_time`, `status`, `create_time`, `update_time`) VALUES
(1, '鍛ㄦ澃浼?024涓栫晫宸″洖婕斿敱浼氬寳浜珯',
    'https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?w=700',
    '鍛ㄦ澃浼?024涓栫晫宸″洖婕斿敱浼氬寳浜珯锛屾椂闅斾笁骞村啀搴︽潵浜紝鐢ㄦ渶椤剁骇鐨勫埗浣溿€佹渶闇囨捈鐨勯煶鍝嶆晥鏋滐紝涓哄寳浜殑姝岃糠甯︽潵涓€鍦鸿鍚洓瀹达紒',
    '鍖椾含楦熷发浣撹偛鍦?, '鍖椾含', '婕斿敱浼?,
    '2024-12-01 19:30:00', '2024-12-01 22:00:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(2, '寮€蹇冮夯鑺辩垎绗戝枩鍓с€婁箤榫欏北浼埖銆?,
    'https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?w=700',
    '寮€蹇冮夯鑺辩粡鍏稿枩鍓с€婁箤榫欏北浼埖銆嬶紝涓€涓棤宸ヤ綔銆佹棤鎴垮瓙銆佹棤杞﹀瓙鐨?涓夋棤"闈掑勾璋㈣煿锛屽湪涓夊崄宀佺敓鏃ヨ繖澶╁拷鐒跺緱鍒颁竴绗斿法棰濋仐浜э紝鍙堣帿鍚嶅叾濡欒璇涓哄姭鍖紝琚揩閫冧骸...',
    '鍖椾含鍦拌川绀煎爞', '鍖椾含', '璇濆墽',
    '2024-11-15 19:30:00', '2024-11-15 21:30:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(3, 'CBA瀛ｅ悗璧涘箍涓淰S杈藉畞',
    'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=700',
    '2023-2024璧涘CBA瀛ｅ悗璧涘崐鍐宠禌锛屽箍涓滃崕鍗楄檸瀵归樀杈藉畞鏈挗锛屽己寮哄璇濓紝璋佸皢鏅嬬骇鍐宠禌锛?,
    '骞垮窞浣撹偛棣?, '骞垮窞', '浣撹偛',
    '2024-11-20 19:35:00', '2024-11-20 21:35:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(4, '钖涗箣璋?澶╁鏉ョ墿"宸″洖婕斿敱浼?,
    'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=700',
    '钖涗箣璋?024宸″洖婕斿敱浼氾紝浠?闊充箰+鍠滃墽"鐨勫垱鏂板舰寮忓憟鐜帮紝铻嶅悎鍚勭闊充箰椋庢牸锛屾墦閫犱竴鍦哄墠鎵€鏈湁鐨勯煶涔愪綋楠屻€?,
    '涓婃捣姊呰禌寰锋柉濂旈┌鏂囧寲涓績', '涓婃捣', '婕斿敱浼?,
    '2024-12-15 19:30:00', '2024-12-15 22:00:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(5, '鍛ㄦ澃浼?024鍙板寳绔?,
    'https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?w=700',
    '鍛ㄦ澃浼?024涓栫晫宸″洖婕斿敱浼氬彴鍖楃珯锛屽洖鍒版渶鍒濇ⅵ鎯冲紑濮嬬殑鍦版柟锛屼笌鍙板寳鐨勬瓕杩峰叡搴﹂毦蹇橀煶涔愪箣澶溿€?,
    '鍙板寳灏忓法铔?, '鍙板寳', '婕斿敱浼?,
    '2024-10-25 19:30:00', '2024-10-25 22:00:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(6, '鍎跨鍓с€婂啺闆缂樸€?,
    'https://picsum.photos/700/700?random=20241220',
    '缁忓吀鍔ㄧ敾銆婂啺闆缂樸€嬫敼缂栬垶鍙板墽锛岃开澹凹姝ｇ増鎺堟潈锛岀粴涓借垶鍙版晥鏋滐紝甯﹀瀛愯繘鍏ユⅵ骞荤殑鍐伴洩涓栫晫锛?,
    '娣卞湷淇濆埄鍓ч櫌', '娣卞湷', '鍎跨浜插瓙',
    '2024-12-20 10:30:00', '2024-12-20 12:00:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(7, '浜旀湀澶?024璇轰簹鏂硅垷婕斿敱浼?,
    'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=700',
    '浜旀湀澶?024涓栫晫宸″洖婕斿敱浼氾紝涔樺潗璇轰簹鏂硅垷锛岀┛瓒婇煶涔愭捣娲嬶紝涓庝綘鐩哥害鏈€濂界殑涓€澶╋紒',
    '棣欐腐绾㈢！浣撹偛棣?, '棣欐腐', '婕斿敱浼?,
    '2024-11-30 19:30:00', '2024-11-30 22:30:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(8, '鑺暰鑸炲墽銆婂ぉ楣呮箹銆?,
    'https://images.unsplash.com/photo-1518834107812-67b0b7c58434?w=700',
    '淇勭綏鏂殗瀹惰姯钑捐垶鍥㈢粡鍏稿憟鐜帮紝鏌村彲澶柉鍩轰紶涓栧悕浣滐紝鎰熷彈鍙ゅ吀鑺暰鐨勬瀬鑷翠紭闆呫€?,
    '鍖椾含鍥藉澶у墽闄?, '鍖椾含', '鑸炶箞',
    '2024-10-10 19:30:00', '2024-10-10 21:30:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(9, '銆屾槦娌冲贰鍥炪€嶆紨鍞变細路涓婃捣绔?,
    'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?w=800&h=1000&fit=crop',
    '鍗庤娴佽婕斿敱浼氾紝鑸炵編涓庨煶鍝嶅叏闈㈠崌绾э紝缁忓吀閲戞洸杩炲敱銆?,
    '姊呰禌寰锋柉-濂旈┌鏂囧寲涓績', '涓婃捣', '婕斿敱浼?,
    '2026-07-18 19:30:00', '2026-07-18 22:00:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(10, '銆婃殫鎭嬫鑺辨簮銆嬬粡鍏歌瘽鍓?,
    'https://images.unsplash.com/photo-1503095396549-807759245b35?w=800&h=1000&fit=crop',
    '璧栧０宸濅唬琛ㄤ綔锛屾偛鍠滀氦閿欑殑鍩庡競瀵撹█锛屽叏鍥藉贰婕旈┗鍦恒€?,
    '鍥藉澶у墽闄⒙锋垙鍓у満', '鍖椾含', '璇濆墽',
    '2026-06-06 19:30:00', '2026-06-06 21:30:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(11, '2026CBA鍏ㄦ槑鏄熷懆鏈?,
    'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800&h=1000&fit=crop',
    '鍗楀寳鍖虹悆鏄熷鎶椼€佹墸绡ぇ璧涗笌涓夊垎澶ц禌锛屼竴绁ㄧ湅鍏ㄣ€?,
    '骞垮窞浣撹偛棣?, '骞垮窞', '浣撹偛',
    '2026-05-01 18:00:00', '2026-05-01 22:00:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(12, '銆屽嵃璞℃淳涓庡厜褰便€嶆矇娴稿紡鑹烘湳灞?,
    'https://picsum.photos/800/1000?random=20260420',
    '鑾銆侀浄璇洪樋绛夊ぇ甯堝鍒讳笌鏁板瓧鍏夊奖浜掑姩锛岄€傚悎鎵撳崱涓庝翰瀛愬叡瑙堛€?,
    '瑗垮哺缇庢湳棣?, '涓婃捣', '灞曡',
    '2026-04-20 10:00:00', '2026-08-31 18:00:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08'),

(13, '銆婃湪鍋跺閬囪銆嬩翰瀛愪簰鍔ㄨ垶鍙板墽',
    'https://images.unsplash.com/photo-1513885535751-8b9238bd345a?w=800&h=1000&fit=crop',
    '缁忓吀绔ヨ瘽鏀圭紪锛屽叏鍦轰簰鍔ㄤ笌灏忓皬宸ヤ綔鍧婏紝寤鸿3宀佷互涓婂搴婕斻€?,
    '涓婃捣鍎跨鑹烘湳鍓у満', '涓婃捣', '鍎跨浜插瓙',
    '2026-06-01 10:30:00', '2026-06-01 12:00:00', 1, '2026-04-21 20:16:08', '2026-04-21 20:16:08');

-- ============================================================================
-- 3.3 绁ㄦ。鏁版嵁 (42鏉?
--     姣忓満婕斿嚭2~5涓エ妗ｏ紝浠锋牸浠庝綆鍒伴珮鎺掑垪
-- ============================================================================
INSERT INTO `biz_ticket` (`id`, `show_id`, `name`, `price`, `total_stock`, `available_stock`, `seat_range`) VALUES
-- 婕斿嚭1: 鍛ㄦ澃浼﹀寳浜珯
(1, 1, 'VIP绁?, 1999.00, 500, 500, '鍐呭満VIP鍖?),
(2, 1, '鐢茬エ', 1299.00, 1000, 1000, '鍐呭満A鍖?),
(3, 1, '涔欑エ',  799.00, 2000, 2000, '鐪嬪彴涓€灞?),
(4, 1, '涓欑エ',  499.00, 3000, 3000, '鐪嬪彴浜屽眰'),
-- 婕斿嚭2: 涔岄緳灞变集鐖?(5, 2, 'VIP绁?,  680.00, 100, 100, '姹犲骇1-5鎺?),
(6, 2, '鐢茬エ',  480.00, 200, 200, '姹犲骇6-15鎺?),
(7, 2, '涔欑エ',  280.00, 300, 300, '妤煎骇'),
-- 婕斿嚭3: CBA瀛ｅ悗璧?(8, 3, '鍐呭満VIP', 1280.00, 200, 200, '鍐呭満鍓嶆帓'),
(9, 3, '鐢茬エ',   880.00, 500, 500, '鍐呭満'),
(10, 3, '涔欑エ',  480.00, 1000, 1000, '鐪嬪彴'),
-- 婕斿嚭4: 钖涗箣璋︿笂娴风珯
(11, 4, 'VIP绁?, 1680.00, 300, 300, '鍐呭満VIP鍖?),
(12, 4, '鐢茬エ', 1080.00, 800, 800, '鍐呭満A鍖?),
(13, 4, '涔欑エ',  680.00, 1500, 1500, '鐪嬪彴'),
-- 婕斿嚭5: 鍛ㄦ澃浼﹀彴鍖楃珯
(14, 5, '鐗笰绁?, 2888.00, 200, 200, '鐗笰鍖猴細1-20鎺?),
(15, 5, '鐢茬エ',  1888.00, 800, 800, 'A鍖猴細21-50鎺?),
(16, 5, '涔欑エ',  1288.00, 1500, 1500, 'B鍖?),
(17, 5, '涓欑エ',   888.00, 2500, 2500, 'C鍖?),
-- 婕斿嚭6: 鍐伴洩濂囩紭
(18, 6, 'VIP浜插瓙绁?, 380.00, 100, 100, 'VIP鍖猴紙涓€澶т竴灏忥級'),
(19, 6, '鐢茬エ',      280.00, 300, 300, 'A鍖?),
(20, 6, '涔欑エ',      180.00, 400, 400, 'B鍖?),
(21, 6, '瀛︾敓绁?,    100.00, 200, 200, '瀛︾敓鍖?),
-- 婕斿嚭7: 浜旀湀澶╅娓珯
(22, 7, 'VIP绁?, 1688.00, 1000, 1000, '鍐呭満VIP'),
(23, 7, '鐢茬エ',  1088.00, 1800, 1800, '鍐呭満A'),
(24, 7, '涔欑エ',   688.00, 2200, 2200, '鍐呭満B'),
(25, 7, '鐪嬪彴绁?, 388.00, 1500, 1500, '鐪嬪彴'),
-- 婕斿嚭8: 澶╅箙婀?(26, 8, 'VIP绁?,   980.00, 150, 150, 'VIP鍖哄墠鎺?),
(27, 8, '鐢茬エ',    680.00, 300, 300, 'A鍖?),
(28, 8, '涔欑エ',    480.00, 400, 400, 'B鍖?),
(29, 8, '瀛︾敓绁?,  280.00, 200, 200, '瀛︾敓鍖?),
-- 婕斿嚭9: 鏄熸渤宸″洖
(30, 9, '鍐呭満VIP', 1288.00,  800,  800, '鍐呭満鍓嶅尯'),
(31, 9, '鐪嬪彴A妗?,  688.00, 2000, 2000, '鐪嬪彴涓€灞?),
(32, 9, '鐪嬪彴B妗?,  388.00, 3500, 3500, '鐪嬪彴浜屽眰'),
-- 婕斿嚭10: 鏆楁亱妗冭姳婧?(33, 10, '涓€绛夊骇', 580.00, 400, 400, '姹犲骇涓尯'),
(34, 10, '浜岀瓑搴?, 380.00, 600, 600, '姹犲骇涓や晶'),
(35, 10, '瀛︾敓绁?, 180.00, 120, 120, '瀛︾敓娲诲姩鍖?),
-- 婕斿嚭11: CBA鍏ㄦ槑鏄?(36, 11, '鍐呭満閫氱エ', 880.00, 3000, 3000, '鍐呭満鍏ㄥ尯鍩?),
(37, 11, '鐪嬪彴绁?,   280.00, 8000, 8000, '鐜舰鐪嬪彴'),
-- 婕斿嚭12: 鍗拌薄娲惧厜褰卞睍
(38, 12, '鏃╅笩閫氱エ',      128.00, 5000, 5000, '鍏ㄥ睍鏈熼€氱敤'),
(39, 12, '浜插瓙濂楃エ锛堜竴澶т竴灏忥級', 198.00, 800, 800, '鍚効绔ュ瑙堝唽'),
-- 婕斿嚭13: 鏈ㄥ伓濂囬亣璁?(40, 13, '浜插瓙搴э紙鍚簰鍔級',  320.00, 260, 260, '鍓嶆帓浜掑姩鍖?),
(41, 13, '瀹跺涵濂楃エ锛堜笁浜猴級',  680.00, 120, 120, '涓尯杩炲骇'),
(42, 13, '鏅€氬骇',           180.00, 400, 400, '鍚庢帓鍖哄煙');

-- ============================================================================
-- 3.4 搴т綅鏁版嵁 (15鏉?
--     浠呭懆鏉颁鸡鍖椾含绔橵IP绁ㄦ。鏈夌ず渚嬪骇浣嶆暟鎹?-- ============================================================================
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
-- 3.5 绉掓潃鍦烘鏁版嵁 (6鏉?
--     娉ㄦ剰锛歴tart_time 鍜?end_time 涓虹鏉€娲诲姩鏃堕棿鑼冨洿
--     褰撳墠鏁版嵁璁句负2030骞达紝琛ㄧず鏈紑濮嬬殑绉掓潃鍦烘
--     濡傞渶娴嬭瘯锛岃灏唖tart_time鏀逛负杩囧幓鏃堕棿
-- ============================================================================
INSERT INTO `biz_seckill_session` (`id`, `show_id`, `ticket_id`, `name`, `seckill_price`, `start_time`, `end_time`, `stock`, `status`, `create_time`, `update_time`) VALUES
(1, 1, 1, '鍛ㄦ澃浼﹀寳浜珯-VIP绉掓潃', 999.00,  '2026-04-21 20:16:08', '2027-04-30 22:16:08', 100, 1, '2026-04-21 20:16:08', '2026-05-07 23:13:36'),
(2, 1, 2, '鍛ㄦ澃浼﹀寳浜珯-鐢茬エ绉掓潃', 699.00,  '2026-04-21 20:16:08', '2027-04-30 22:16:08', 200, 1, '2026-04-21 20:16:08', '2026-05-07 23:13:41'),
(3, 1, 3, '鍛ㄦ澃浼﹀寳浜珯-涔欑エ绉掓潃', 399.00,  '2026-04-23 20:16:08', '2027-04-30 22:16:08', 300, 1, '2026-04-21 20:16:08', '2026-05-07 23:13:44'),
(4, 4, 11, '钖涗箣璋﹀寳浜珯-VIP绉掓潃', 899.00, '2026-04-24 20:16:08', '2027-04-30 22:16:08', 80,  1, '2026-04-21 20:16:08', NULL),
(5, 5, 14, '鍛ㄦ澃浼﹀彴鍖楃珯-鐗笰绉掓潃', 1888.00,'2026-04-26 20:16:08', '2027-04-30 22:16:08', 50,  1, '2026-04-21 20:16:08', '2026-05-07 23:12:59'),
(6, 5, 15, '鍛ㄦ澃浼﹀彴鍖楃珯-鐢茬エ绉掓潃', 988.00, '2026-04-26 20:16:08', '2027-04-30 22:16:08', 100, 1, '2026-04-21 20:16:08', NULL);

-- ============================================================================
-- 3.6 璁㈠崟鏁版嵁锛堝綋鍓嶄负绌猴級
--     璁㈠崟閫氳繃绯荤粺涓嬪崟鎺ュ彛鍔ㄦ€佺敓鎴愶紝姝ゅ涓嶉缃祴璇曟暟鎹?-- ============================================================================


-- ============================================================================
-- XXX  閲嶇疆鑷涓婚敭锛堢‘淇濇柊鎻掑叆鏁版嵁鐨処D鎸夐鏈熷闀匡級
-- ============================================================================
-- 濡傛灉涓嶉渶瑕侀噸缃富閿捣濮嬪€硷紝娉ㄩ噴鎺変互涓嬭鍙?-- ALTER TABLE sys_user            AUTO_INCREMENT = 4;
-- ALTER TABLE biz_show            AUTO_INCREMENT = 14;
-- ALTER TABLE biz_ticket          AUTO_INCREMENT = 43;
-- ALTER TABLE biz_seat            AUTO_INCREMENT = 16;
-- ALTER TABLE biz_seckill_session AUTO_INCREMENT = 7;
-- ALTER TABLE biz_order           AUTO_INCREMENT = 1;
