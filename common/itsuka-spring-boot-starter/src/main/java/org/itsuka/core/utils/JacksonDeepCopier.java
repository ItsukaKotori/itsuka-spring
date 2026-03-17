package org.itsuka.core.utils;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

@RequiredArgsConstructor
public class JacksonDeepCopier {
    private final JsonMapper mapper ;

    /**
     * 深拷贝对象
     * 使用 @SuppressWarnings 抑制类型安全警告，因为我们通过 source.getClass() 
     * 确保了类型的一致性
     */
    @SuppressWarnings("unchecked")
    public <T> T copy(T source) {
        try {
            String json = mapper.writeValueAsString(source);
            return (T) mapper.readValue(json, source.getClass());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 深拷贝对象 - 类型安全版本
     * 调用者显式传入目标类型，避免类型安全警告
     */
    public <T> T copyWithClass(T source, Class<T> targetClass) {
        try {
            String json = mapper.writeValueAsString(source);
            return mapper.readValue(json, targetClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
