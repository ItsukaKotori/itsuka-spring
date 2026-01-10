package org.itsuka.web.desensitization;

import java.util.regex.Pattern;

/**
 * 脱敏工具类
 * 提供各种类型的脱敏处理方法
 *
 * @author Itsuka
 * @since 2023/12/7
 */
public class DesensitizationUtils {

    /**
     * 邮箱正则表达式
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^(.+)@(.+)$");

    /**
     * 手机号正则表达式
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 身份证号正则表达式
     */
    private static final Pattern ID_CARD_PATTERN = Pattern
            .compile("^[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");

    /**
     * 银行卡号正则表达式
     */
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("^\\d{16,19}$");

    /**
     * 固定电话正则表达式
     */
    private static final Pattern FIXED_PHONE_PATTERN = Pattern.compile("^0\\d{2,3}-?\\d{7,8}$");

    /**
     * 根据脱敏类型处理字符串
     *
     * @param origin     原始字符串
     * @param type       脱敏类型
     * @param prefixKeep 保留前缀长度
     * @param suffixKeep 保留后缀长度
     * @param replacer   替换字符
     * @return 脱敏后的字符串
     */
    public static String desensitize(String origin, DesensitizationType type, int prefixKeep, int suffixKeep,
            char replacer) {
        if (origin == null || origin.isEmpty()) {
            return origin;
        }

        switch (type) {
            case USERNAME:
                return usernameDesensitization(origin, replacer);
            case CHINESE_NAME:
                return chineseNameDesensitization(origin, replacer);
            case ID_CARD:
                return idCardDesensitization(origin, replacer);
            case PHONE:
                return phoneDesensitization(origin, replacer);
            case FIXED_PHONE:
                return fixedPhoneDesensitization(origin, replacer);
            case EMAIL:
                return emailDesensitization(origin, replacer);
            case BANK_CARD:
                return bankCardDesensitization(origin, replacer);
            case PASSWORD:
                return passwordDesensitization(origin, replacer);
            case ADDRESS:
                return addressDesensitization(origin, replacer);
            case CUSTOM:
                return customDesensitization(origin, prefixKeep, suffixKeep, replacer);
            default:
                return origin;
        }
    }

    /**
     * 用户名脱敏
     *
     * @param username 用户名
     * @param replacer 替换字符
     * @return 脱敏后的用户名
     */
    public static String usernameDesensitization(String username, char replacer) {
        if (username == null || username.length() <= 3) {
            return username;
        }
        int prefixKeep = 2;
        int suffixKeep = 1;
        return customDesensitization(username, prefixKeep, suffixKeep, replacer);
    }

    /**
     * 中文姓名脱敏
     *
     * @param name     姓名
     * @param replacer 替换字符
     * @return 脱敏后的姓名
     */
    public static String chineseNameDesensitization(String name, char replacer) {
        if (name == null || name.length() <= 1) {
            return name;
        }
        int prefixKeep = 1;
        int suffixKeep = 0;
        return customDesensitization(name, prefixKeep, suffixKeep, replacer);
    }

    /**
     * 身份证号脱敏
     *
     * @param idCard   身份证号
     * @param replacer 替换字符
     * @return 脱敏后的身份证号
     */
    public static String idCardDesensitization(String idCard, char replacer) {
        if (idCard == null || idCard.length() <= 10) {
            return idCard;
        }
        int prefixKeep = 6;
        int suffixKeep = 4;
        return customDesensitization(idCard, prefixKeep, suffixKeep, replacer);
    }

    /**
     * 手机号脱敏
     *
     * @param phone    手机号
     * @param replacer 替换字符
     * @return 脱敏后的手机号
     */
    public static String phoneDesensitization(String phone, char replacer) {
        if (phone == null || phone.length() <= 7) {
            return phone;
        }
        int prefixKeep = 3;
        int suffixKeep = 4;
        return customDesensitization(phone, prefixKeep, suffixKeep, replacer);
    }

    /**
     * 固定电话脱敏
     *
     * @param phone    固定电话
     * @param replacer 替换字符
     * @return 脱敏后的固定电话
     */
    public static String fixedPhoneDesensitization(String phone, char replacer) {
        if (phone == null || phone.length() <= 6) {
            return phone;
        }
        int prefixKeep = 4;
        int suffixKeep = 2;
        return customDesensitization(phone, prefixKeep, suffixKeep, replacer);
    }

    /**
     * 邮箱脱敏
     *
     * @param email    邮箱
     * @param replacer 替换字符
     * @return 脱敏后的邮箱
     */
    public static String emailDesensitization(String email, char replacer) {
        if (email == null) {
            return email;
        }

        java.util.regex.Matcher matcher = EMAIL_PATTERN.matcher(email);
        if (!matcher.matches()) {
            return email;
        }

        String prefix = matcher.group(1);
        String suffix = matcher.group(2);

        if (prefix.length() <= 2) {
            return email;
        }

        String desensitizedPrefix = customDesensitization(prefix, 2, 0, replacer);
        return desensitizedPrefix + "@" + suffix;
    }

    /**
     * 银行卡号脱敏
     *
     * @param bankCard 银行卡号
     * @param replacer 替换字符
     * @return 脱敏后的银行卡号
     */
    public static String bankCardDesensitization(String bankCard, char replacer) {
        if (bankCard == null || bankCard.length() <= 8) {
            return bankCard;
        }
        int prefixKeep = 4;
        int suffixKeep = 4;
        return customDesensitization(bankCard, prefixKeep, suffixKeep, replacer);
    }

    /**
     * 密码脱敏
     *
     * @param password 密码
     * @param replacer 替换字符
     * @return 脱敏后的密码
     */
    public static String passwordDesensitization(String password, char replacer) {
        if (password == null) {
            return password;
        }

        if (password.isEmpty()) {
            return password;
        }

        return String.valueOf(replacer).repeat(password.length());
    }

    /**
     * 地址脱敏
     *
     * @param address  地址
     * @param replacer 替换字符
     * @return 脱敏后的地址
     */
    public static String addressDesensitization(String address, char replacer) {
        if (address == null || address.length() <= 6) {
            return address;
        }
        int prefixKeep = 6;
        int suffixKeep = 0;
        return customDesensitization(address, prefixKeep, suffixKeep, replacer);
    }

    /**
     * 自定义脱敏
     *
     * @param origin     原始字符串
     * @param prefixKeep 保留前缀长度
     * @param suffixKeep 保留后缀长度
     * @param replacer   替换字符
     * @return 脱敏后的字符串
     */
    public static String customDesensitization(String origin, int prefixKeep, int suffixKeep, char replacer) {
        if (origin == null || origin.isEmpty()) {
            return origin;
        }

        int length = origin.length();

        // 如果保留的前后长度之和大于等于字符串长度，则不做处理
        if (prefixKeep + suffixKeep >= length) {
            return origin;
        }

        // 计算需要替换的字符数
        int replaceCount = length - prefixKeep - suffixKeep;

        // 构建脱敏后的字符串
        StringBuilder sb = new StringBuilder();

        // 添加前缀
        if (prefixKeep > 0) {
            sb.append(origin, 0, prefixKeep);
        }

        // 添加替换字符
        for (int i = 0; i < replaceCount; i++) {
            sb.append(replacer);
        }

        // 添加后缀
        if (suffixKeep > 0) {
            sb.append(origin, length - suffixKeep, length);
        }

        return sb.toString();
    }

    /**
     * 检查是否为邮箱
     *
     * @param email 待检查的字符串
     * @return 是否为邮箱
     */
    public static boolean isEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 检查是否为手机号
     *
     * @param phone 待检查的字符串
     * @return 是否为手机号
     */
    public static boolean isPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 检查是否为身份证号
     *
     * @param idCard 待检查的字符串
     * @return 是否为身份证号
     */
    public static boolean isIdCard(String idCard) {
        return idCard != null && ID_CARD_PATTERN.matcher(idCard).matches();
    }

    /**
     * 检查是否为银行卡号
     *
     * @param bankCard 待检查的字符串
     * @return 是否为银行卡号
     */
    public static boolean isBankCard(String bankCard) {
        return bankCard != null && BANK_CARD_PATTERN.matcher(bankCard).matches();
    }

    /**
     * 检查是否为固定电话
     *
     * @param fixedPhone 待检查的字符串
     * @return 是否为固定电话
     */
    public static boolean isFixedPhone(String fixedPhone) {
        return fixedPhone != null && FIXED_PHONE_PATTERN.matcher(fixedPhone).matches();
    }
}