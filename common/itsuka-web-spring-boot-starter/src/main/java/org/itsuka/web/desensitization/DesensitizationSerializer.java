package org.itsuka.web.desensitization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * 脱敏序列化器
 * 基于Jackson实现字段脱敏功能
 *
 * @author Itsuka
 * @since 2023/12/7
 */
public class DesensitizationSerializer extends StdSerializer<String> implements ContextualSerializer {

    /**
     * 脱敏类型
     */
    private DesensitizationType type;

    /**
     * 自定义脱敏时保留的前缀长度
     */
    private int prefixKeep;

    /**
     * 自定义脱敏时保留的后缀长度
     */
    private int suffixKeep;

    /**
     * 脱敏替换字符
     */
    private char replacer;

    /**
     * 是否启用脱敏
     */
    private boolean enabled;

    /**
     * 无参构造函数
     */
    public DesensitizationSerializer() {
        super(String.class);
    }

    /**
     * 构造函数
     *
     * @param properties 脱敏配置属性
     */
    public DesensitizationSerializer(DesensitizationProperties properties) {
        super(String.class);
        this.type = DesensitizationType.CUSTOM;
        this.prefixKeep = properties.getDefaultPrefixKeep();
        this.suffixKeep = properties.getDefaultSuffixKeep();
        this.replacer = properties.getReplacer();
        this.enabled = properties.isEnabled();
    }

    /**
     * 构造函数
     *
     * @param type       脱敏类型
     * @param prefixKeep 保留前缀长度
     * @param suffixKeep 保留后缀长度
     * @param replacer   替换字符
     * @param enabled    是否启用脱敏
     */
    public DesensitizationSerializer(DesensitizationType type, int prefixKeep, int suffixKeep, char replacer,
            boolean enabled) {
        super(String.class);
        this.type = type;
        this.prefixKeep = prefixKeep;
        this.suffixKeep = suffixKeep;
        this.replacer = replacer;
        this.enabled = enabled;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null || !enabled) {
            gen.writeString(value);
            return;
        }

        String desensitizedValue = DesensitizationUtils.desensitize(value, type, prefixKeep, suffixKeep, replacer);
        gen.writeString(desensitizedValue);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        if (property == null) {
            return this;
        }

        // 获取字段上的Desensitization注解
        Desensitization annotation = property.getAnnotation(Desensitization.class);
        if (annotation == null) {
            // 如果字段上没有注解，检查类上是否有注解
            annotation = property.getContextAnnotation(Desensitization.class);
        }

        if (annotation == null || !annotation.enabled()) {
            // 如果没有注解或注解禁用，返回默认的字符串序列化器
            return new DesensitizationSerializer() {
                @Override
                public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                    gen.writeString(value);
                }
            };
        }

        // 创建新的序列化器实例
        return new DesensitizationSerializer(
                annotation.type(),
                annotation.prefixKeep(),
                annotation.suffixKeep(),
                annotation.replacer(),
                true);
    }
}