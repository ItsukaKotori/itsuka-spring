package org.itsuka.core.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反射工具类
 * 提供各种反射操作的便捷方法
 */
public class ReflectionKit {

    /**
     * 字段缓存，避免重复查找
     */
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 方法缓存，避免重复查找
     */
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 包装类型映射
     */
    private static final Set<Class<?>> WRAPPER_TYPES = Set.of(
        Boolean.class, Byte.class, Character.class, Short.class,
        Integer.class, Long.class, Float.class, Double.class, Void.class
    );

    /**
     * 私有构造函数，防止实例化
     */
    private ReflectionKit() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 创建类的实例
     * 
     * @param clazz 类对象
     * @param <T>   类型泛型
     * @return 类的实例
     * @throws IllegalArgumentException 如果类为null
     * @throws RuntimeException 如果创建实例失败
     */
    public static <T> T newInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        }
    }

    /**
     * 通过构造函数创建类的实例
     * 
     * @param constructor 构造函数
     * @param args        构造参数
     * @param <T>         类型泛型
     * @return 类的实例
     * @throws IllegalArgumentException 如果构造函数为null
     * @throws RuntimeException 如果创建实例失败
     */
    public static <T> T newInstance(Constructor<T> constructor, Object... args) {
        if (constructor == null) {
            throw new IllegalArgumentException("Constructor cannot be null");
        }
        return executeWithAccessible(constructor, null, () -> constructor.newInstance(args),
            "Failed to create instance using constructor");
    }

    /**
     * 使用可访问性模式执行操作的通用方法
     * 
     * @param accessible 可访问对象
     * @param target 目标对象（对于静态成员为null）
     * @param operation 要执行的操作
     * @param errorMessage 错误消息
     * @param <T> 返回类型
     * @return 操作结果
     */
    private static <T> T executeWithAccessible(AccessibleObject accessible, Object target, 
                                               AccessibleOperation<T> operation, String errorMessage) {
        try {
            boolean wasAccessible = accessible.canAccess(target);
            if (!wasAccessible) {
                accessible.trySetAccessible();
            }
            return operation.execute();
        } catch (Exception e) {
            throw new RuntimeException(errorMessage, e);
        }
    }
    
    /**
     * 可访问操作接口
     */
    @FunctionalInterface
    private interface AccessibleOperation<T> {
        T execute() throws Exception;
    }

    /**
     * 获取类的所有字段（包括父类）
     * 
     * @param clazz 类对象
     * @return 字段列表
     * @throws IllegalArgumentException 如果类为null
     */
    public static List<Field> getFields(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            Collections.addAll(fields, currentClass.getDeclaredFields());
            currentClass = currentClass.getSuperclass();
        }
        return fields;
    }

    /**
     * 获取类的所有方法（包括父类）
     * 
     * @param clazz 类对象
     * @return 方法列表
     * @throws IllegalArgumentException 如果类为null
     */
    public static List<Method> getMethods(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        List<Method> methods = new ArrayList<>();
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            Collections.addAll(methods, currentClass.getDeclaredMethods());
            currentClass = currentClass.getSuperclass();
        }
        return methods;
    }

    /**
     * 获取类的所有构造函数
     * 
     * @param clazz 类对象
     * @return 构造函数列表
     * @throws IllegalArgumentException 如果类为null
     */
    public static List<Constructor<?>> getConstructors(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        return Arrays.asList(clazz.getDeclaredConstructors());
    }

    /**
     * 获取指定名称的字段（支持缓存）
     * 
     * @param clazz     类对象
     * @param fieldName 字段名
     * @return 字段对象
     * @throws IllegalArgumentException 如果参数为null
     * @throws RuntimeException 如果找不到字段
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }
        
        String cacheKey = clazz.getName() + "#" + fieldName;
        return FIELD_CACHE.computeIfAbsent(cacheKey, k -> findField(clazz, fieldName));
    }
    
    /**
     * 查找字段的内部方法
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        throw new RuntimeException("Field not found: " + fieldName + " in class " + clazz.getName());
    }

    /**
     * 获取指定名称的方法（支持缓存）
     * 
     * @param clazz      类对象
     * @param methodName 方法名
     * @param paramTypes 参数类型
     * @return 方法对象
     * @throws IllegalArgumentException 如果参数为null
     * @throws RuntimeException 如果找不到方法
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        if (methodName == null || methodName.trim().isEmpty()) {
            throw new IllegalArgumentException("Method name cannot be null or empty");
        }
        
        String cacheKey = clazz.getName() + "#" + methodName + "#" + Arrays.toString(paramTypes);
        return METHOD_CACHE.computeIfAbsent(cacheKey, k -> findMethod(clazz, methodName, paramTypes));
    }
    
    /**
     * 查找方法的内部方法
     */
    private static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredMethod(methodName, paramTypes);
            } catch (NoSuchMethodException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        throw new RuntimeException("Method not found: " + methodName + " in class " + clazz.getName());
    }

    /**
     * 获取字段的值
     * 
     * @param obj       对象实例
     * @param fieldName 字段名
     * @return 字段值
     * @throws IllegalArgumentException 如果参数为null
     * @throws RuntimeException 如果获取失败
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object obj, String fieldName) {
        if (obj == null) {
            throw new IllegalArgumentException("Object cannot be null");
        }
        Field field = getField(obj.getClass(), fieldName);
        return executeWithAccessible(field, obj, () -> (T) field.get(obj),
            "Failed to get field value: " + fieldName);
    }
    
    /**
     * 获取字段的值，支持指定默认值
     * 
     * @param obj       对象实例
     * @param fieldName 字段名
     * @param defaultValue 默认值
     * @return 字段值，如果获取失败则返回默认值
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValueOrDefault(Object obj, String fieldName, T defaultValue) {
        try {
            return getFieldValue(obj, fieldName);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 设置字段的值
     * 
     * @param obj       对象实例
     * @param fieldName 字段名
     * @param value     字段值
     * @throws IllegalArgumentException 如果对象为null
     * @throws RuntimeException 如果设置失败
     */
    public static void setFieldValue(Object obj, String fieldName, Object value) {
        if (obj == null) {
            throw new IllegalArgumentException("Object cannot be null");
        }
        Field field = getField(obj.getClass(), fieldName);
        executeWithAccessible(field, obj, () -> {
            field.set(obj, value);
            return null;
        }, "Failed to set field value: " + fieldName);
    }


    
    /**
     * 调用方法
     * 
     * @param obj       对象实例
     * @param methodName 方法名
     * @param paramTypes 参数类型
     * @param args      方法参数
     * @param <T>       返回值类型泛型
     * @return 方法返回值
     * @throws IllegalArgumentException 如果对象为null
     * @throws RuntimeException 如果调用失败
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(Object obj, String methodName, Class<?>[] paramTypes, Object... args) {
        if (obj == null) {
            throw new IllegalArgumentException("Object cannot be null");
        }
        Method method = getMethod(obj.getClass(), methodName, paramTypes);
        return executeWithAccessible(method, obj, () -> (T) method.invoke(obj, args),
            "Failed to invoke method: " + methodName);
    }


    
    /**
     * 调用静态方法
     * 
     * @param clazz      类对象
     * @param methodName 方法名
     * @param paramTypes 参数类型
     * @param args       方法参数
     * @param <T>        返回值类型泛型
     * @return 方法返回值
     * @throws IllegalArgumentException 如果类为null
     * @throws RuntimeException 如果调用失败
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeStaticMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes, Object... args) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        Method method = getMethod(clazz, methodName, paramTypes);
        return executeWithAccessible(method, null, () -> (T) method.invoke(null, args),
            "Failed to invoke static method: " + methodName);
    }

    /**
     * 检查类是否包含指定注解
     * 
     * @param clazz       类对象
     * @param annotationClass 注解类
     * @return 是否包含注解
     */
    public static boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        return clazz.isAnnotationPresent(annotationClass);
    }

    /**
     * 获取类的所有注解
     * 
     * @param clazz 类对象
     * @return 注解数组
     */
    public static Annotation[] getAnnotations(Class<?> clazz) {
        return clazz.getAnnotations();
    }

    /**
     * 获取指定类型的注解
     * 
     * @param clazz       类对象
     * @param annotationClass 注解类
     * @param <A>         注解类型泛型
     * @return 注解对象
     * @throws IllegalArgumentException 如果参数为null
     */
    public static <A extends Annotation> A getAnnotation(Class<?> clazz, Class<A> annotationClass) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        if (annotationClass == null) {
            throw new IllegalArgumentException("Annotation class cannot be null");
        }
        return clazz.getAnnotation(annotationClass);
    }

    /**
     * 获取类的父类
     * 
     * @param clazz 类对象
     * @return 父类对象
     */
    public static Class<?> getSuperclass(Class<?> clazz) {
        return clazz.getSuperclass();
    }

    /**
     * 获取类实现的所有接口
     * 
     * @param clazz 类对象
     * @return 接口数组
     */
    public static Class<?>[] getInterfaces(Class<?> clazz) {
        return clazz.getInterfaces();
    }

    /**
     * 判断类型是否为基本类型或其包装类型
     * 
     * @param clazz 类对象
     * @return 是否为基本类型或包装类型
     * @throws IllegalArgumentException 如果类为null
     */
    public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        return clazz.isPrimitive() || WRAPPER_TYPES.contains(clazz);
    }

    /**
     * 判断类型是否为集合类型
     * 
     * @param clazz 类对象
     * @return 是否为集合类型
     * @throws IllegalArgumentException 如果类为null
     */
    public static boolean isCollection(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        return Collection.class.isAssignableFrom(clazz);
    }

    /**
     * 判断类型是否为Map类型
     * 
     * @param clazz 类对象
     * @return 是否为Map类型
     * @throws IllegalArgumentException 如果类为null
     */
    public static boolean isMap(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        return Map.class.isAssignableFrom(clazz);
    }

    /**
     * 判断类型是否为数组类型
     * 
     * @param clazz 类对象
     * @return 是否为数组类型
     * @throws IllegalArgumentException 如果类为null
     */
    public static boolean isArray(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        return clazz.isArray();
    }

    /**
     * 判断类型是否为枚举类型
     * 
     * @param clazz 类对象
     * @return 是否为枚举类型
     * @throws IllegalArgumentException 如果类为null
     */
    public static boolean isEnum(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        return clazz.isEnum();
    }

    /**
     * 判断类型是否为字符串类型
     * 
     * @param clazz 类对象
     * @return 是否为字符串类型
     * @throws IllegalArgumentException 如果类为null
     */
    public static boolean isString(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        return String.class.isAssignableFrom(clazz);
    }

    /**
     * 获取泛型类型
     * 
     * @param type 类型对象
     * @return 泛型类型数组
     * @throws IllegalArgumentException 如果类型为null
     */
    public static Class<?>[] getGenericTypes(Type type) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        if (type instanceof ParameterizedType parameterizedType) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            return Arrays.stream(actualTypeArguments)
                    .filter(Class.class::isInstance)
                    .map(Class.class::cast)
                    .toArray(Class<?>[]::new);
        }
        return new Class<?>[0];
    }

    /**
     * 获取字段的泛型类型
     * 
     * @param field 字段对象
     * @return 泛型类型数组
     * @throws IllegalArgumentException 如果字段为null
     */
    public static Class<?>[] getGenericTypes(Field field) {
        if (field == null) {
            throw new IllegalArgumentException("Field cannot be null");
        }
        return getGenericTypes(field.getGenericType());
    }

    /**
     * 检查字段是否为final类型
     * 
     * @param field 字段对象
     * @return 是否为final类型
     */
    public static boolean isFinal(Field field) {
        return Modifier.isFinal(field.getModifiers());
    }

    /**
     * 检查字段是否为static类型
     * 
     * @param field 字段对象
     * @return 是否为static类型
     */
    public static boolean isStatic(Field field) {
        return Modifier.isStatic(field.getModifiers());
    }

    /**
     * 检查字段是否为transient类型
     * 
     * @param field 字段对象
     * @return 是否为transient类型
     */
    public static boolean isTransient(Field field) {
        return Modifier.isTransient(field.getModifiers());
    }

    /**
     * 检查方法是否为synchronized类型
     * 
     * @param method 方法对象
     * @return 是否为synchronized类型
     */
    public static boolean isSynchronized(Method method) {
        return Modifier.isSynchronized(method.getModifiers());
    }
    
    /**
     * 检查方法是否为final类型
     * 
     * @param method 方法对象
     * @return 是否为final类型
     */
    public static boolean isFinal(Method method) {
        return Modifier.isFinal(method.getModifiers());
    }
    
    /**
     * 检查方法是否为static类型
     * 
     * @param method 方法对象
     * @return 是否为static类型
     */
    public static boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    /**
     * 获取类的所有内部类
     * 
     * @param clazz 类对象
     * @return 内部类数组
     */
    public static Class<?>[] getInnerClasses(Class<?> clazz) {
        return clazz.getDeclaredClasses();
    }

    /**
     * 获取类的包名
     * 
     * @param clazz 类对象
     * @return 包名
     * @throws IllegalArgumentException 如果类为null或包信息不存在
     */
    public static String getPackageName(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        Package pkg = clazz.getPackage();
        if (pkg == null) {
            throw new IllegalArgumentException("Package information not available for class: " + clazz.getName());
        }
        return pkg.getName();
    }

    /**
     * 获取类的简单名称
     * 
     * @param clazz 类对象
     * @return 简单名称
     */
    public static String getSimpleName(Class<?> clazz) {
        return clazz.getSimpleName();
    }

    /**
     * 获取类的全限定名称
     * 
     * @param clazz 类对象
     * @return 全限定名称
     */
    public static String getClassName(Class<?> clazz) {
        return clazz.getName();
    }


    
    /**
     * 将对象转换为Map（字段名->字段值）
     * 
     * @param obj 对象实例
     * @param ignoreError 是否忽略无法获取的字段
     * @return 字段名到字段值的映射
     * @throws IllegalArgumentException 如果对象为null
     */
    public static Map<String, Object> toMap(Object obj, boolean ignoreError) {
        if (obj == null) {
            return new HashMap<>();
        }
        Map<String, Object> map = new HashMap<>();
        List<Field> fields = getFields(obj.getClass());
        for (Field field : fields) {
            // 跳过static、final和transient字段
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || Modifier.isTransient(modifiers)) {
                continue;
            }
            try {
                Object value = executeWithAccessible(field, obj, () -> field.get(obj),
                    "Failed to get field value: " + field.getName());
                map.put(field.getName(), value);
            } catch (Exception e) {
                if (!ignoreError) {
                    throw new RuntimeException("Failed to get field value: " + field.getName(), e);
                }
            }
        }
        return map;
    }


    
    /**
     * 将Map转换为对象
     * 
     * @param map   字段名到字段值的映射
     * @param clazz 目标类
     * @param ignoreError 是否忽略无法设置的字段
     * @param <T>   类型泛型
     * @return 对象实例
     * @throws IllegalArgumentException 如果类为null
     */
    public static <T> T fromMap(Map<String, Object> map, Class<T> clazz, boolean ignoreError) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        if (map == null || map.isEmpty()) {
            return newInstance(clazz);
        }
        T instance = newInstance(clazz);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            try {
                setFieldValue(instance, entry.getKey(), entry.getValue());
            } catch (Exception e) {
                if (!ignoreError) {
                    throw new RuntimeException("Failed to set field value: " + entry.getKey(), e);
                }
            }
        }
        return instance;
    }
    
    /**
     * 安全地获取类对象，避免ClassNotFoundException
     * 
     * @param className 类名
     * @return 类对象，如果不存在则返回null
     * @throws IllegalArgumentException 如果类名为null或空
     */
    public static Class<?> safeGetClass(String className) {
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("Class name cannot be null or empty");
        }
        try {
            return Class.forName(className.trim());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    /**
     * 获取字段上的指定注解
     * 
     * @param field 字段对象
     * @param annotationClass 注解类
     * @param <A> 注解类型泛型
     * @return 注解对象，如果不存在则返回null
     * @throws IllegalArgumentException 如果参数为null
     */
    public static <A extends Annotation> A getAnnotation(Field field, Class<A> annotationClass) {
        if (field == null) {
            throw new IllegalArgumentException("Field cannot be null");
        }
        if (annotationClass == null) {
            throw new IllegalArgumentException("Annotation class cannot be null");
        }
        return field.getAnnotation(annotationClass);
    }
    
    /**
     * 获取方法上的指定注解
     * 
     * @param method 方法对象
     * @param annotationClass 注解类
     * @param <A> 注解类型泛型
     * @return 注解对象，如果不存在则返回null
     * @throws IllegalArgumentException 如果参数为null
     */
    public static <A extends Annotation> A getAnnotation(Method method, Class<A> annotationClass) {
        if (method == null) {
            throw new IllegalArgumentException("Method cannot be null");
        }
        if (annotationClass == null) {
            throw new IllegalArgumentException("Annotation class cannot be null");
        }
        return method.getAnnotation(annotationClass);
    }
    
    /**
     * 清空缓存（主要用于测试或内存管理）
     */
    public static void clearCache() {
        FIELD_CACHE.clear();
        METHOD_CACHE.clear();
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计信息
     */
    public static Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("fieldCacheSize", FIELD_CACHE.size());
        stats.put("methodCacheSize", METHOD_CACHE.size());
        return stats;
    }
}
