package org.itsuka.dict.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.core.utils.ReflectionKit;
import org.itsuka.dict.annotation.DictField;
import org.itsuka.dict.annotation.DictModel;
import org.itsuka.dict.cache.DictCacheManager;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 字典响应处理器
 * 用于处理响应实体中的字典字段，当实体中存在@DictField及DictModel注解时，从缓存中获取字典值替换对应的字段值
 */
@Slf4j
@RequiredArgsConstructor
public class DictResponseHandler {
    private final DictCacheManager dictCacheManager;
    
    /**
     * 处理响应对象中的字典字段
     * @param responseObject 响应对象
     * @return 处理后的响应对象
     */
    public Object processDictFields(Object responseObject) {
        if (responseObject == null) {
            return null;
        }

        // 处理集合类型
        if (responseObject instanceof Collection<?> collection) {
            Collection<Object> resultCollection = new ArrayList<>(collection.size());
            for (Object item : collection) {
                resultCollection.add(processDictFields(item));
            }
            return resultCollection;
        }

        // 处理Map类型
        if (responseObject instanceof Map<?, ?> map) {
            Map<Object, Object> resultMap = new HashMap<>(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                resultMap.put(entry.getKey(), processDictFields(entry.getValue()));
            }
            return resultMap;
        }

        // 处理数组类型
        if (responseObject.getClass().isArray()) {
            Object[] array = (Object[]) responseObject;
            Object[] resultArray = new Object[array.length];
            for (int i = 0; i < array.length; i++) {
                resultArray[i] = processDictFields(array[i]);
            }
            return resultArray;
        }

        // 处理基本类型和包装类型
        if (ReflectionKit.isPrimitiveOrWrapper(responseObject.getClass()) || ReflectionKit.isString(responseObject.getClass())) {
            return responseObject;
        }

        // 处理自定义对象类型
        return processObjectDictFields(responseObject);
    }

    /**
     * 处理对象中的字典字段
     * @param object 目标对象
     * @return 处理后的对象
     */
    private Object processObjectDictFields(Object object) {
        if (object == null) {
            return null;
        }
        Class<?> clazz = object.getClass();
        
        // 使用 ReflectionKit 获取注解
        DictModel dictModel = ReflectionKit.getAnnotation(clazz, DictModel.class);
        String prefix = dictModel != null ? dictModel.prefix() : "";

        // 使用 ReflectionKit 获取所有字段（包括父类）
        List<Field> fields = ReflectionKit.getFields(clazz);

        for (Field field : fields) {
            // 使用 ReflectionKit 获取字段注解
            DictField dictField = ReflectionKit.getAnnotation(field, DictField.class);
            if (dictField != null) {
                try {
                    // 使用 ReflectionKit 获取字段值
                    Object fieldValue = ReflectionKit.getFieldValue(object, field.getName());
                    if (fieldValue != null) {
                        // 构建字典key
                        String dictKey = buildDictKey(prefix, dictField.key(), fieldValue.toString());
                        // 从缓存中获取字典值
                        String dictValue = dictCacheManager.get(dictKey, String.class);
                        if (dictValue == null || dictValue.isEmpty()) {
                            // 如果缓存中没有值，使用默认值
                            dictValue = dictField.defaultValue();
                        }
                        
                        // 根据配置决定是否覆盖原值或设置文本字段
                        if (dictField.override() && (dictValue != null && !dictValue.isEmpty())) {
                            // 覆盖原字段值
                            ReflectionKit.setFieldValue(object, field.getName(), dictValue);
                        } else {
                            // 设置字典值到对应的文本字段（假设字段名为fieldNameText）
                            setDictTextValue(object, field.getName(), dictValue);
                        }
                    }
                } catch (Exception e) {
                    // 使用日志记录错误，但不影响主流程
                    log.warn("处理字典字段 {} 时出现异常", field.getName(), e);
                }
            }
        }

        return object;
    }

    /**
     * 构建字典key
     * @param prefix 前缀
     * @param key 字典key
     * @param value 字典值
     * @return 构建后的字典key
     */
    private String buildDictKey(String prefix, String key, String value) {
        StringBuilder sb = new StringBuilder();
        if (prefix != null && !prefix.isEmpty()) {
            sb.append(prefix).append(":");
        }
        sb.append(key).append(":").append(value);
        return sb.toString();
    }

    /**
     * 设置字典文本值到对象中
     * @param object 目标对象
     * @param fieldName 原字段名
     * @param dictValue 字典文本值
     */
    private void setDictTextValue(Object object, String fieldName, String dictValue) {
        if (dictValue == null || dictValue.isEmpty()) {
            return;
        }
        
        try {
            // 假设字典文本字段名为原字段名加上Text后缀
            String textFieldName = fieldName + "Text";
            
            // 使用 ReflectionKit 查找字段，如果不存在则返回
            try {
                ReflectionKit.setFieldValue(object, textFieldName, dictValue);
                log.debug("成功设置字典文本字段 {} = {}", textFieldName, dictValue);
            } catch (RuntimeException e) {
                // 字段不存在，跳过
                log.debug("字典文本字段 {} 不存在，跳过设置", textFieldName);
            }
        } catch (Exception e) {
            // 处理异常，但不影响主流程
            log.warn("设置字典文本字段 {} 时出现异常", fieldName, e);
        }
    }


}