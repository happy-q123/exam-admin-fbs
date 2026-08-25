package com.domain.annotation;

import java.lang.annotation.*;

/**
 * 审计日志注解
 *
 * @author zzq
 * @date 2026-06-11
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audit {

    /**
     * 操作内容描述
     */
    String value() default "";
}
