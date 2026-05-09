package com.jsu.common.util;

import java.util.Random;
import java.util.UUID;

/**
 * 字符串工具类
 * 继承 org.apache.commons.lang3.StringUtils，获得丰富的字符串处理功能
 *
 * 自定义功能：
 * - generateUUID()：生成32位无横杠UUID
 * - generateOrderNo()：生成订单号（ORD + 时间戳 + 6位随机数）
 * - generateCaptcha()：生成数字验证码
 * - maskPhone()：手机号脱敏（138****1234）
 * - maskEmail()：邮箱脱敏（joh****@example.com）
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils {

    /**
     * 生成32位UUID字符串（去掉横杠）
     *
     * @return 32位十六进制字符
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成订单号
     * 格式：ORD + 13位时间戳 + 6位随机数
     * 例如：ORD20260507123456123456
     * 保证全局唯一
     *
     * @return 订单号
     */
    public static String generateOrderNo() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.format("%06d", new Random().nextInt(1000000));
        return "ORD" + timestamp + random;
    }

    /**
     * 生成数字验证码
     *
     * @param length 验证码长度
     * @return 纯数字验证码
     */
    public static String generateCaptcha(int length) {
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(new Random().nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 手机号脱敏处理
     * 保留前3后4，中间替换为****
     * 例如：13812345678 → 138****5678
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (isBlank(phone) || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 邮箱脱敏处理
     * 用户名部分保留前3位，后面替换为****
     * 例如：john.doe@example.com → joh****@example.com
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (isBlank(email) || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String username = parts[0];
        if (username.length() <= 3) {
            return username + "****@" + parts[1];
        }
        return username.substring(0, 3) + "****@" + parts[1];
    }
}
