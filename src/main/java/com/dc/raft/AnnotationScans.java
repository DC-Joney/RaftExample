package com.dc.raft;

import cn.hutool.core.lang.ClassScanner;
import cn.hutool.core.lang.Filter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AnnotationScans {

    private static final Map<Class<? extends Annotation>, Set<Class<?>>> annotationCache = new HashMap<>();

    /**
     * 扫描annotation 相关的class
     *
     * @param annotationClass 注解的class
     * @param interfaceClass  注解所在的class
     * @param packages        package名称
     */
    public static Set<Class<?>> scan(Class<? extends Annotation> annotationClass, Class<?> interfaceClass, String... packages) {
        Set<Class<?>> annotationClasses = annotationCache.get(annotationClass);
        //优先从缓存中获取
        if (!CollectionUtils.isEmpty(annotationClasses)) {
            return annotationClasses;
        }

        synchronized (annotationClass) {
            Set<Class<?>> classes = new HashSet<>();
            for (String instancePackage : packages) {
                Set<Class<?>> instanceClasses = ClassScanner.scanPackage(instancePackage, new Filter<Class<?>>() {
                    @Override
                    public boolean accept(Class<?> instanceClass) {
                        return AnnotationUtils.isAnnotationDeclaredLocally(annotationClass, instanceClass)
                                && ClassUtils.isAssignable(interfaceClass, instanceClass);
                    }
                });
                classes.addAll(instanceClasses);
            }
            return classes;
        }
    }

}
