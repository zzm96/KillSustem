package com.zzm.kill.server.aspect;

import com.zzm.kill.api.enums.StatusCode;
import com.zzm.kill.api.response.BaseResponse;
import com.zzm.kill.server.annotation.RateLimter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

/**
 * @author zzm
 * @data 2020/6/12 8:58
 */

@Component
@Aspect
public class RedisLimterAspect {
    @Autowired
    private RRateLimiter rateLimiter;


    @Pointcut("execution(* com.zzm.kill.server.controller.KillController.*(..))")
    private void pointCut() {
    }


    @Around("pointCut()")
    public Object process(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        System.out.println("切入点");

        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();

        RateLimter declaredAnnotation = signature.getMethod().getDeclaredAnnotation(RateLimter.class);
        if (declaredAnnotation == null) return proceedingJoinPoint.proceed();

        int value = declaredAnnotation.value();

        // 尝试获取1个令牌，尝试等待时间为3秒钟
        boolean getChance = rateLimiter.tryAcquire(1, 3, TimeUnit.SECONDS);
        if(getChance) return proceedingJoinPoint.proceed();

        return new BaseResponse(StatusCode.Fail.getCode(), "服务器繁忙  稍后重试");
    }
}
