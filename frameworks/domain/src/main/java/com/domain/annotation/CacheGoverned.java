package com.domain.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识可安全缓存的计算型方法。缓存键由切面结合参数、模型和业务命名空间生成。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheGoverned {
    String namespace();

    long ttlSeconds() default 300;
}
