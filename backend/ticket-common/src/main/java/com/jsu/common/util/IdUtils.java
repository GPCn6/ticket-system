package com.jsu.common.util;

import java.util.Random;

public class IdUtils {

    private static final Random RANDOM = new Random();
    private static final String CHARS = "0123456789";

    /**
     * 生成雪花ID
     * 注意：这里使用简化版，实际生产环境建议使用分布式ID生成器
     */
    public static long generateSnowflakeId() {
        long timestamp = System.currentTimeMillis();
        long machineId = 1L; // 机器ID
        long sequence = RANDOM.nextInt(1000); // 序列号
        return (timestamp << 22) | (machineId << 12) | sequence;
    }

    /**
     * 生成用户ID
     */
    public static long generateUserId() {
        return generateSnowflakeId();
    }

    /**
     * 生成演出ID
     */
    public static long generateShowId() {
        return generateSnowflakeId();
    }

    /**
     * 生成票档ID
     */
    public static long generateTicketId() {
        return generateSnowflakeId();
    }

    /**
     * 生成订单ID
     */
    public static long generateOrderId() {
        return generateSnowflakeId();
    }

    /**
     * 生成座位ID
     */
    public static long generateSeatId() {
        return generateSnowflakeId();
    }

    /**
     * 生成抢购场次ID
     */
    public static long generateSeckillSessionId() {
        return generateSnowflakeId();
    }

    /**
     * 生成随机数字字符串
     */
    public static String generateRandomNumber(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
