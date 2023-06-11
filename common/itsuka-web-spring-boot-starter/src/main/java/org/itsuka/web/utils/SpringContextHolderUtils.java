package org.itsuka.web.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Spring Bean 获取工具类
 *
 * @author Itsuka
 * @date 2023/3/1
 */
@Slf4j
public class SpringContextHolderUtils implements ApplicationContextAware {

    private static ApplicationContext beanHolder;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.debug("applicationContext init");
        beanHolder = applicationContext;
    }

    /**
     * 获取某一类型的Bean列表
     * 
     * @param <T>
     * @param beanType
     * @return
     */
    public static <T> Collection<T> getBeansOfType(Class<T> beanType) {
        return beanHolder.getBeansOfType(beanType).values();
    }

    /**
     * 根据类型获取Bean
     *
     * @param tClass 类型
     * @return Bean
     */
    public static <T> T getBean(Class<T> tClass) {
        return beanHolder.getBean(tClass);
    }

    /**
     * 根据BeanName获取Bean
     *
     * @param beanName bean名称
     * @return Bean
     */
    public static Object getBean(String beanName) {
        return beanHolder.getBean(beanName);
    }
}
