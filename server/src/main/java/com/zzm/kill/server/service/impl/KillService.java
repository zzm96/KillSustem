package com.zzm.kill.server.service.impl;

import com.zzm.kill.model.entity.ItemKill;
import com.zzm.kill.model.entity.ItemKillSuccess;
import com.zzm.kill.model.mapper.ItemKillMapper;
import com.zzm.kill.model.mapper.ItemKillSuccessMapper;
import com.zzm.kill.model.utils.RandomUtil;
import com.zzm.kill.model.utils.SnowFlake;
import com.zzm.kill.server.service.IKillService;
import com.zzm.kill.server.service.RabbitSenderService;
import com.zzm.kill.server.service.enums.SysConstant;
import org.joda.time.DateTime;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author zzm
 * @data 2020/6/8 9:56
 */
@Service
public class KillService implements IKillService {

    private static final Logger log = LoggerFactory.getLogger(KillService.class);

    private SnowFlake snowFlake = new SnowFlake(2, 3);

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    @Autowired
    private ItemKillMapper itemKillMapper;

    @Autowired
    private RabbitSenderService rabbitSenderService;

    @Override
    public Boolean killItem(Integer killId, Integer userId) throws Exception {
        Boolean result = false;

        //TODO:判断当前用户是否已经抢购过当前商品
        if (itemKillSuccessMapper.countByKillUserId(killId, userId) <= 0) {
            //TODO:查询待秒杀商品详情
            ItemKill itemKill = itemKillMapper.selectById(killId);

            //TODO:判断是否可以被秒杀canKill=1?
            if (itemKill != null && 1 == itemKill.getCanKill()) {
                //TODO:扣减库存-减一 返回匹配行数，id唯一，这里指影响的行数
                int res = itemKillMapper.updateKillItem(killId);

                //TODO:扣减是否成功?是-生成秒杀成功的订单，同时通知用户秒杀成功的消息
                if (res == 1) {
                    commonRecordKillSuccessInfo(itemKill,userId);
                    result=true;
                }
            }
        }else {
            throw new Exception("已经抢购过该商品了");
        }
        return result;
    }



    /**
     * 通用的方法-记录用户秒杀成功后生成的订单-并进行异步邮件消息的通知
     * @param kill
     * @param userId
     * @throws Exception
     */
    private void commonRecordKillSuccessInfo(ItemKill kill,Integer userId) throws Exception{
        //TODO:记录抢购成功后生成的秒杀订单记录

        ItemKillSuccess entity=new ItemKillSuccess();
        String orderNo=String.valueOf(snowFlake.nextId());

        //entity.setCode(RandomUtil.generateOrderCode());   //传统时间戳+N位随机数
        entity.setCode(orderNo); //雪花算法
        entity.setItemId(kill.getItemId());
        entity.setKillId(kill.getId());
        entity.setUserId(userId.toString());
        entity.setStatus(SysConstant.OrderStatus.SuccessNotPayed.getCode().byteValue());
        entity.setCreateTime(DateTime.now().toDate());
        //TODO:学以致用，举一反三 -> 仿照单例模式的双重检验锁写法
        if (itemKillSuccessMapper.countByKillUserId(kill.getId(),userId) <= 0){
            int res=itemKillSuccessMapper.insertSelective(entity);

            if (res>0){
                //TODO:进行异步邮件消息的通知=rabbitmq+mail
                rabbitSenderService.sendKillSuccessEmailMsg(orderNo);

                //TODO:入死信队列，用于 “失效” 超过指定的TTL时间时仍然未支付的订单
                rabbitSenderService.sendKillSuccessOrderExpireMsg(orderNo);
            }
        }
    }


    /**
     * 商品秒杀核心业务逻辑的处理-mysql的优化
     * @param killId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public Boolean killItemV2(Integer killId, Integer userId) throws Exception {
        Boolean result=false;

        //TODO:判断当前用户是否已经抢购过当前商品
        if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){
            //TODO:A.查询待秒杀商品详情
            ItemKill itemKill=itemKillMapper.selectByIdV2(killId);

            //TODO:判断是否可以被秒杀canKill=1?
            if (itemKill!=null && 1==itemKill.getCanKill() && itemKill.getTotal()>0){
                //TODO:B.扣减库存-减一  剩余库存大于1时再减库存
                int res=itemKillMapper.updateKillItemV2(killId);

                //TODO:扣减是否成功?是-生成秒杀成功的订单，同时通知用户秒杀成功的消息
                if (res>0){
                    commonRecordKillSuccessInfo(itemKill,userId);
                    result=true;
                }
            }
        }else{
            throw new Exception("您已经抢购过该商品了!");
        }
        return result;
    }


    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 商品秒杀核心业务逻辑的处理-redis的分布式锁
     * @param killId
     * @param userId
     * @return
     * @throws Exception
     * 逻辑：利用redis加入 订单key-随机value， key存在则不抢购；抢购则加入key，设置超时时间,避免订单结束没能删除key
     */
    @Override
    public Boolean killItemV3(Integer killId, Integer userId) throws Exception {
        Boolean result=false;

        //TODO:判断当前用户是否已经抢购过当前商品
        if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){

            ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
            final String key = new StringBuffer().append(userId).append(killId).append("-RedisLock").toString();
            final String value = RandomUtil.generateOrderCode();
            Boolean cacheRes = valueOperations.setIfAbsent(key, value);
            //TOOD:redis部署节点宕机了
            if(cacheRes){
                stringRedisTemplate.expire(key,30, TimeUnit.SECONDS);

                try{
                    //TODO:A.查询待秒杀商品详情
                    ItemKill itemKill=itemKillMapper.selectByIdV2(killId);

                    //TODO:判断是否可以被秒杀canKill=1?
                    if (itemKill!=null && 1==itemKill.getCanKill()){
                        //TODO:B.扣减库存-减一  剩余库存大于1时再减库存
                        int res=itemKillMapper.updateKillItemV2(killId);

                        //TODO:扣减是否成功?是-生成秒杀成功的订单，同时通知用户秒杀成功的消息
                        if (res>0){
                            commonRecordKillSuccessInfo(itemKill,userId);
                            result=true;
                        }
                    }
                }catch (Exception e){
                    throw new Exception("抢购失败！");
                }finally {
                    if (value.equals(valueOperations.get(key).toString())){
                        stringRedisTemplate.delete(key);
                    }
                }
            }

        }else{
            throw new Exception("您已经抢购过该商品了!");
        }
        return result;
    }

    @Autowired
    private RedissonClient redissonClient;
    @Override
    public Boolean killItemV4(Integer killId, Integer userId) throws Exception {
        Boolean result=false;

        final String lockKey = new StringBuffer().append(killId).append(userId).append("-RedisssonLock").toString();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            //最多等待时长和自动解锁时间长度
            boolean cacheRes = lock.tryLock(30, 10, TimeUnit.SECONDS);
            if(cacheRes){
                //TODO:判断当前用户是否已经抢购过当前商品
                if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){
                    //TODO:A.查询待秒杀商品详情
                    ItemKill itemKill=itemKillMapper.selectByIdV2(killId);

                    //TODO:判断是否可以被秒杀canKill=1?
                    if (itemKill!=null && 1==itemKill.getCanKill() && itemKill.getTotal()>0){
                        //TODO:B.扣减库存-减一  剩余库存大于1时再减库存
                        int res=itemKillMapper.updateKillItemV2(killId);

                        //TODO:扣减是否成功?是-生成秒杀成功的订单，同时通知用户秒杀成功的消息
                        if (res>0){
                            commonRecordKillSuccessInfo(itemKill,userId);
                            result=true;
                        }
                    }
                }else{
                    throw new Exception("您已经抢购过该商品了!");
                }
            }
        }finally {
            lock.unlock();
        }
        return result;
    }

    @Override
    public Boolean killItemV5(Integer killId, Integer userId) throws Exception {
        return null;
    }
}
