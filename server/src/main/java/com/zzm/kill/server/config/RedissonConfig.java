package com.zzm.kill.server.config;/**
 * Created by Administrator on 2019/7/2.
 */

import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.concurrent.TimeUnit;

/**
 * redisson通用化配置
 * @Author:debug (SteadyJack)
 * @Date: 2019/7/2 10:57
 **/
@Configuration
public class RedissonConfig {

    @Autowired
    private Environment env;

    @Bean
    public RedissonClient redissonClient(){

        Config config=new Config();
        config.useSingleServer()
                .setAddress(env.getProperty("redis.config.host"));
        RedissonClient client= Redisson.create(config);
        return client;
    }


    @Bean
    public RRateLimiter limter() {
        RRateLimiter rateLimiter = redissonClient().getRateLimiter("myRateLimiter");
        // 初始化
        // 最大流速 = 每1秒钟产生10个令牌  从配置文件读取
        rateLimiter.trySetRate(RateType.OVERALL, Long.parseLong(env.getProperty("residdon.rateLimiter")), 1, RateIntervalUnit.SECONDS);
        return  rateLimiter;
    }
}