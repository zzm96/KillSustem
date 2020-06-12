package com.zzm.kill.server.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author zzm
 * @data 2020/6/12 8:54
 */
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimter {
    @AliasFor("limte")//别名
    int value() default Integer.MAX_VALUE;

    int limit() default Integer.MAX_VALUE;
}
