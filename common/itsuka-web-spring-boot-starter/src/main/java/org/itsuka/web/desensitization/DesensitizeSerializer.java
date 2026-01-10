package org.itsuka.web.desensitization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;
import java.util.Objects;

/**
 * 脱敏序列化器
 * 基于Jackson序列化器实现字段脱敏
 *
 * @author Itsuka
 * @since 2023/12/10
 */
public class DesensitizeSerializer extends JsonSerializer<String> implements ContextualSerializer {

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
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null || value.isEmpty()) {
            gen.writeString(value);
            return;
        }

        String desensitizedValue = DesensitizationUtils.desensitize(value, type, prefixKeep, suffixKeep, replacer);
        gen.writeString(desensitizedValue);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
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