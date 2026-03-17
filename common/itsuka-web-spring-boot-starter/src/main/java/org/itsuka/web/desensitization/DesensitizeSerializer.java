package org.itsuka.web.desensitization;


import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Objects;

/**
 * 脱敏序列化器
 * 基于Jackson序列化器实现字段脱敏
 *
 * @author Itsuka
 * @since 2023/12/10
 */
public class DesensitizeSerializer extends ValueSerializer<String> {

    /**
     * 脱敏类型
     */
    private DesensitizeType type;

    /**
     * 自定义脱敏时，保留前几位
     */
    private int prefixKeep;

    /**
     * 自定义脱敏时，保留后几位
     */
    private int suffixKeep;

    /**
     * 自定义脱敏时，使用的替换字符
     */
    private char replacer;

    /**
     * 无参构造器
     */
    public DesensitizeSerializer() {
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        if (value == null || value.isEmpty()) {
            gen.writeString(value);
            return;
        }

        String desensitizedValue = DesensitizationUtils.desensitize(value, type, prefixKeep, suffixKeep, replacer);
        gen.writeString(desensitizedValue);
    }

    /**
     * 有参构造器
     *
     * @param type       脱敏类型
     * @param prefixKeep 保留前几位
     * @param suffixKeep 保留后几位
     * @param replacer   替换字符
     */
    public DesensitizeSerializer(DesensitizeType type, int prefixKeep, int suffixKeep, char replacer) {
        this.type = type;
        this.prefixKeep = prefixKeep;
        this.suffixKeep = suffixKeep;
        this.replacer = replacer;
    }


    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt,
                                               BeanProperty property) {
        if (property == null) {
            return this;
        }

        Desensitize annotation = property.getAnnotation(Desensitize.class);
        if (annotation == null) {
            annotation = property.getContextAnnotation(Desensitize.class);
        }

        if (annotation == null) {
            return this;
        }

        return new DesensitizeSerializer(
                annotation.type(),
                annotation.prefixKeep(),
                annotation.suffixKeep(),
                annotation.replacer());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DesensitizeSerializer that = (DesensitizeSerializer) o;
        return prefixKeep == that.prefixKeep &&
                suffixKeep == that.suffixKeep &&
                replacer == that.replacer &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, prefixKeep, suffixKeep, replacer);
    }
}