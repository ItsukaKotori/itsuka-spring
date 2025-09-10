package org.itsuka.dict.interceptor;

import org.itsuka.dict.annotation.DictField;
import org.itsuka.dict.annotation.DictModel;
import org.itsuka.dict.cache.DictCacheManager;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 字典响应处理器
 * 用于处理响应实体中的字典字段，当实体中存在@DictField及DictModel注解时，从缓存中获取字典值替换对应的字段值
 */
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
        if (responseObject instanceof String || responseObject instanceof Number || 
            responseObject instanceof Boolean || responseObject instanceof Character) {
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
        DictModel dictModel = clazz.getAnnotation(DictModel.class);
        String prefix = dictModel != null ? dictModel.prefix() : "";

        // 获取所有字段，包括父类的字段
        List<Field> fields = getAllFields(clazz);

        for (Field field : fields) {
            DictField dictField = field.getAnnotation(DictField.class);
            if (dictField != null) {
                try {
                    field.setAccessible(true);
                    Object fieldValue = field.get(object);
                    if (fieldValue != null) {
                        // 构建字典key
                        String dictKey = buildDictKey(prefix, dictField.key(), fieldValue.toString());
                        // 从缓存中获取字典值
                        String dictValue = dictCacheManager.get(dictKey, String.class);
                        if (dictValue == null || dictValue.isEmpty()) {
                            // 如果缓存中没有值，使用默认值
                            dictValue = dictField.defaultValue();
                        }
                        // 设置字典值到对应的文本字段（假设字段名为fieldNameText）
                        setDictTextValue(object, field.getName(), dictValue);
                    }
                } catch (Exception e) {
                    // 处理异常，但不影响主流程
                    e.printStackTrace();
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
        try {
            // 假设字典文本字段名为原字段名加上Text后缀
            String textFieldName = fieldName + "Text";
            Field textField = findField(object.getClass(), textFieldName);
            if (textField != null) {
                textField.setAccessible(true);
                textField.set(object, dictValue);
            }
        } catch (Exception e) {
            // 处理异常，但不影响主流程
            e.printStackTrace();
        }
    }

    /**
     * 获取类的所有字段，包括父类的字段
     * @param clazz 类
     * @return 字段列表
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * 查找类中的指定字段
     * @param clazz 类
     * @param fieldName 字段名
     * @return 字段对象，如果不存在则返回null
     */
    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}