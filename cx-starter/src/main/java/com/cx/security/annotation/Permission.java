package com.cx.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 功能权限注解
 * 标注在 Service 类或方法上，配合 AOP 实现权限拦截
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Permission {

    /**
     * 权限编码，如 "order:view"
     */
    String code();

    /**
     * 所属模块，若类上标注则作为默认模块
     */
    String module() default "";

    /**
     * 权限名称
     */
    String name() default "";

    /**
     * 权限描述
     */
    String desc() default "";
}