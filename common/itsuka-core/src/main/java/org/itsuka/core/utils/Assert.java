package org.itsuka.core.utils;

import org.itsuka.core.exception.ServiceException;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 断言工具类
 * 提供各种断言方法，失败时抛出ServiceException
 * 
 * @author Itsuka
 */
public class Assert {
    
    /**
     * 私有构造函数，防止实例化
     */
    private Assert() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * 断言表达式为真
     * 
     * @param expression 表达式
     * @param message 错误消息
     * @throws ServiceException 如果表达式为假
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw ServiceException.create(message);
        }
    }
    
    /**
     * 断言表达式为真（带错误码）
     * 
     * @param expression 表达式
     * @param code 错误码
     * @param message 错误消息
     * @throws ServiceException 如果表达式为假
     */
    public static void isTrue(boolean expression, Integer code, String message) {
        if (!expression) {
            throw ServiceException.create(code, message);
        }
    }
    
    /**
     * 断言表达式为真（懒加载消息）
     * 
     * @param expression 表达式
     * @param messageSupplier 错误消息供应商
     * @throws ServiceException 如果表达式为假
     */
    public static void isTrue(boolean expression, Supplier<String> messageSupplier) {
        if (!expression) {
            throw ServiceException.create(messageSupplier.get());
        }
    }
    
    /**
     * 断言表达式为假
     * 
     * @param expression 表达式
     * @param message 错误消息
     * @throws ServiceException 如果表达式为真
     */
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw ServiceException.create(message);
        }
    }
    
    /**
     * 断言对象不为null
     * 
     * @param object 对象
     * @param message 错误消息
     * @throws ServiceException 如果对象为null
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw ServiceException.create(message);
        }
    }
    
    /**
     * 断言对象不为null（带错误码）
     * 
     * @param object 对象
     * @param code 错误码
     * @param message 错误消息
     * @throws ServiceException 如果对象为null
     */
    public static void notNull(Object object, Integer code, String message) {
        if (object == null) {
            throw ServiceException.create(code, message);
        }
    }
    
    /**
     * 断言字符串不为空（null或空字符串）
     * 
     * @param text 字符串
     * @param message 错误消息
     * @throws ServiceException 如果字符串为空
     */
    public static void hasText(String text, String message) {
        if (text == null || text.trim().isEmpty()) {
            throw ServiceException.create(message);
        }
    }
    
    /**
     * 断言字符串不为空（带错误码）
     * 
     * @param text 字符串
     * @param code 错误码
     * @param message 错误消息
     * @throws ServiceException 如果字符串为空
     */
    public static void hasText(String text, Integer code, String message) {
        if (text == null || text.trim().isEmpty()) {
            throw ServiceException.create(code, message);
        }
    }
    
    /**
     * 断言集合不为空
     * 
     * @param collection 集合
     * @param message 错误消息
     * @throws ServiceException 如果集合为null或空
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw ServiceException.create(message);
        }
    }
    
    /**
     * 断言集合不为空（带错误码）
     * 
     * @param collection 集合
     * @param code 错误码
     * @param message 错误消息
     * @throws ServiceException 如果集合为null或空
     */
    public static void notEmpty(Collection<?> collection, Integer code, String message) {
        if (collection == null || collection.isEmpty()) {
            throw ServiceException.create(code, message);
        }
    }
    
    /**
     * 断言数组不为空
     * 
     * @param array 数组
     * @param message 错误消息
     * @throws ServiceException 如果数组为null或空
     */
    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) {
            throw ServiceException.create(message);
        }
    }
    
    /**
     * 断言Map不为空
     * 
     * @param map Map对象
     * @param message 错误消息
     * @throws ServiceException 如果Map为null或空
     */
    public static void notEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) {
            throw ServiceException.create(message);
        }
    }
    
    /**
     * 断言两个对象相等
     * 
     * @param expected 期望值
     * @param actual 实际值
     * @param message 错误消息
     * @throws ServiceException 如果对象不相等
     */
    public static void equals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw ServiceException.create(message);
        }
    }
    
    /**
     * 断言数字大于0
     * 
     * @param number 数字
     * @param message 错误消息
     * @throws ServiceException 如果数字小于等于0
     */
    public static void positive(Number number, String message) {
        notNull(number, message);
        if (number.doubleValue() <= 0) {
            throw ServiceException.create(message);
        }
    }
    
    /**
     * 状态断言 - 用于验证对象状态
     * 
     * @param expression 状态表达式
     * @param message 错误消息
     * @throws ServiceException 如果状态不正确
     */
    public static void state(boolean expression, String message) {
        if (!expression) {
            throw ServiceException.create(message);
        }
    }
    
    /**
     * 状态断言（带错误码）
     * 
     * @param expression 状态表达式
     * @param code 错误码
     * @param message 错误消息
     * @throws ServiceException 如果状态不正确
     */
    public static void state(boolean expression, Integer code, String message) {
        if (!expression) {
            throw ServiceException.create(code, message);
        }
    }
}