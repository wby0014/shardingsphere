package com.xiaoying.common.encrypt.jdbc.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 上下文工具类
 * @author binyu.wu
 * @date 2022/11/1 11:00
 */
public class EncryptApplicationContext implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        EncryptApplicationContext.applicationContext = applicationContext;
    }

    /**
     * 获取bean
     */
    public static <T> T getBean(Class<T> clazz) {
        T obj;
        try {
            obj = applicationContext.getBean(clazz);
        } catch (Exception e) {
            obj = null;
        }
        return obj;
    }

    /**
     * 根据名称获取bean
     */
    public static <T> T getBean(String beanName,Class<T> clazz) {
        T obj;
        try {
            obj = applicationContext.getBean(beanName,clazz);
        } catch (Exception e) {
            obj = null;
        }
        return obj;
    }


    /**
     * 获取 bean 的类型
     */
    public static <T> List<T> getBeansOfType(Class<T> clazz) {
        Map<String, T> map;
        try {
            map = applicationContext.getBeansOfType(clazz);
        } catch (Exception e) {
            map = null;
        }
        return map == null ? null : new ArrayList<>(map.values());
    }


    /**
     * 获取所有被注解的 bean
     */
    public static Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotation) {
        Map<String, Object> map;
        try {
            map = applicationContext.getBeansWithAnnotation(annotation);
        } catch (Exception e) {
            map = null;
        }
        return map;
    }
}
