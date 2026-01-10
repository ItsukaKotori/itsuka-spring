package org.itsuka.web.desensitization;

/**
 * 脱敏工具类
 * 提供各种脱敏规则的具体实现
 *
 * @author Itsuka
 * @since 2023/12/10
 */
public class DesensitizationUtils {

    /**
     * 对字符串进行脱敏处理
     *
     * @param value      原始字符串
     * @param type       脱敏类型
     * @param prefixKeep 保留前几位
     * @param suffixKeep 保留后几位
     * @param replacer   替换字符
     * @return 脱敏后的字符串
     */
    public static String desensitize(String value, DesensitizeType type, int prefixKeep, int suffixKeep,
            char replacer) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        switch (type) {
            case USERNAME:
                return desensitizeUsername(value);
            case PHONE:
                return desensitizePhone(value);
            case EMAIL:
                return desensitizeEmail(value);
            case ID_CARD:
                return desensitizeIdCard(value);
            case BANK_CARD:
                return desensitizeBankCard(value);
            case PASSWORD:
                return desensitizePassword(value, replacer);
            case ADDRESS:
                return desensitizeAddress(value);
            case CUSTOM:
                return desensitizeCustom(value, prefixKeep, suffixKeep, replacer);
            default:
                return value;
        }
    }

    /**
     * 用户名脱敏
     * 规则：保留第一个字符，其余用*代替
     *
     * @param username 用户名
     * @return 脱敏后的用户名
     */
    private static String desensitizeUsername(String username) {
        if (username == null || username.isEmpty()) {
            return username;
        }
        if (username.length() == 1) {
            return username;
        }
        return username.charAt(0) + "*".repeat(username.length() - 1);
    }

    /**
     * 手机号脱敏
     * 规则：保留前3位和后4位，中间用*代替
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    private static String desensitizePhone(String phone) {
        if (phone == null || phone.length() <= 7) {
            return phone;
        }
        return phone.substring(0, 3) + "*".repeat(phone.length() - 7) + phone.substring(phone.length() - 4);
    }

    /**
     * 邮箱脱敏
     * 规则：邮箱前缀仅显示第一个字母，@前及后续域名显示
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    private static String desensitizeEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return email;
        }

        String prefix = parts[0];
        String suffix = parts[1];

        if (prefix.isEmpty()) {
            return email;
        }

        return prefix.charAt(0) + "*".repeat(Math.max(0, prefix.length() - 1)) + "@" + suffix;
    }

    /**
     * 身份证号脱敏
     * 规则：保留前6位和后4位，中间用*代替
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    private static String desensitizeIdCard(String idCard) {
        if (idCard == null || idCard.length() <= 10) {
            return idCard;
        }
        return idCard.substring(0, 6) + "*".repeat(idCard.length() - 10) + idCard.substring(idCard.length() - 4);
    }

    /**
     * 银行卡号脱敏
     * 规则：保留前6位和后4位，中间用*代替
     *
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    private static String desensitizeBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() <= 10) {
            return bankCard;
        }
        return bankCard.substring(0, 6) + "*".repeat(bankCard.length() - 10)
                + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 密码脱敏
     * 规则：全部用*代替
     *
     * @param password 密码
     * @param replacer 替换字符
     * @return 脱敏后的密码
     */
    private static String desensitizePassword(String password, char replacer) {
        if (password == null) {
            return password;
        }
        return String.valueOf(replacer).repeat(password.length());
    }

    /**
     * 地址脱敏
     * 规则：从第6位开始隐藏
     *
     * @param address 地址
     * @return 脱敏后的地址
     */
    private static String desensitizeAddress(String address) {
        if (address == null || address.length() <= 6) {
            return address;
        }
        return address.substring(0, 6) + "*".repeat(address.length() - 6);
    }

    /**
     * 自定义脱敏
     * 规则：保留前几位和后几位，中间用指定字符代替
     *
     * @param value      原始字符串
     * @param prefixKeep 保留前几位
     * @param suffixKeep 保留后几位
     * @param replacer   替换字符
     * @return 脱敏后的字符串
     */
    private static String desensitizeCustom(String value, int prefixKeep, int suffixKeep, char replacer) {
        if (value == null) {
            return value;
        }

        int length = value.length();

        // 如果保留的位数总和大于等于字符串长度，则不进行脱敏
        if (prefixKeep + suffixKeep >= length) {
            return value;
        }

        // 确保保留位数不为负数
        prefixKeep = Math.max(0, prefixKeep);
        suffixKeep = Math.max(0, suffixKeep);

        // 如果保留的位数总和大于等于字符串长度，则不进行脱敏
        if (prefixKeep + suffixKeep >= length) {
            return value;
        }

        String prefix = prefixKeep > 0 ? value.substring(0, prefixKeep) : "";
        String suffix = suffixKeep > 0 ? value.substring(length - suffixKeep) : "";

        int replaceCount = length - prefixKeep - suffixKeep;
        String middle = String.valueOf(replacer).repeat(replaceCount);

        return prefix + middle + suffix;
    }
}