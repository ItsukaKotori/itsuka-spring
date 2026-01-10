package org.itsuka.web.desensitization;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 脱敏类型枚举
 * 定义常用的脱敏处理方式
 *
 * @author Itsuka
 * @since 2023/12/7
 */
@RequiredArgsConstructor
@Getter
public enum DesensitizationType {
    /**
     * 用户名脱敏，保留前2位和后1位，中间用*代替
     * 例如：张三 -> 张*，李四光 -> 李**光
     */
    USERNAME(2, 1),

    /**
     * 中文姓名脱敏，保留前1位，其余用*代替
     * 例如：张三 -> 张*，李四光 -> 李**
     */
    CHINESE_NAME(1, 0),

    /**
     * 身份证号脱敏，保留前6位和后4位，中间用*代替
     * 例如：110101199001011234 -> 110101********1234
     */
    ID_CARD(6, 4),

    /**
     * 手机号脱敏，保留前3位和后4位，中间用*代替
     * 例如：13812345678 -> 138****5678
     */
    PHONE(3, 4),

    /**
     * 固定电话脱敏，保留前4位和后2位，中间用*代替
     * 例如：010-12345678 -> 010-*****78
     */
    FIXED_PHONE(4, 2),

    /**
     * 邮箱脱敏，保留前2位和@后内容，中间用*代替
     * 例如：zhangsan@example.com -> zh******@example.com
     */
    EMAIL(2, 0),

    /**
     * 银行卡号脱敏，保留前4位和后4位，中间用*代替
     * 例如：6222020200012345678 -> 6222************5678
     */
    BANK_CARD(4, 4),

    /**
     * 密码脱敏，全部用*代替
     */
    PASSWORD(0, 0),

    /**
     * 地址脱敏，保留前6位，其余用*代替
     * 例如：北京市朝阳区建国路88号 -> 北京市朝阳区********
     */
    ADDRESS(6, 0),

    /**
     * 自定义脱敏，需要指定保留的前后位数
     */
    CUSTOM(0, 0);

    private final int prefixKeep;
    private final int suffixKeep;
}