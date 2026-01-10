package org.itsuka.web.desensitization;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * 脱敏模块
 * Jackson模块，用于注册脱敏序列化器
 *
 * @author Itsuka
 * @since 2023/12/7
 */
public class DesensitizationModule extends SimpleModule {

    /**
     * 构造函数
     *
     * @param properties 脱敏配置属性
     */
    public DesensitizationModule(DesensitizationProperties properties) {
        super("DesensitizationModule");

        // 添加脱敏序列化器，传入配置属性
        // 注意：这里使用ContextualSerializer，只有在字段上有@Desensitization注解时才会进行脱敏
        addSerializer(String.class, new DesensitizationSerializer(properties));
    }
}