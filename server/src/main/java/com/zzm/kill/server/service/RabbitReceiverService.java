package com.zzm.kill.server.service;

import com.zzm.kill.model.dto.KillSuccessUserInfo;
import com.zzm.kill.model.entity.ItemKillSuccess;
import com.zzm.kill.model.mapper.ItemKillSuccessMapper;
import com.zzm.kill.server.dto.MailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * @author zzm
 * @data 2020/6/10 18:17
 */
@Service
public class RabbitReceiverService {

    private static final Logger log = LoggerFactory.getLogger(RabbitReceiverService.class);

    @Autowired
    private MailService mailService;

    @Autowired
    private Environment env;

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    /**
     * 接收消息-发送邮件通知
     *
     * @param info
     */
    @RabbitListener(containerFactory = "multiListenerContainer", queues = {"${mq.kill.item.success.email.queue}"})
    public void consumeEmail(KillSuccessUserInfo info) {
        try {
            log.info("秒杀异步邮件通知-接收消息:{}", info);


            //TODO:真正的发送邮件....
//            MailDto dto = new MailDto(env.getProperty("mail.kill.item.success.subject"), "这是测试内容", new String[]{info.getEmail()});
//            mailService.sendSimpleEmail(dto);

            final String content=String.format(env.getProperty("mail.kill.item.success.content"),info.getItemName(),info.getCode());
            MailDto dto=new MailDto(env.getProperty("mail.kill.item.success.subject"),content,new String[]{info.getEmail()});
            mailService.sendHTMLMail(dto);

        } catch (Exception e) {
            log.error("秒杀异步邮件通知-接收消息-发生异常：", e.fillInStackTrace());
        }
    }

    /**
     * 用户秒杀成功后超时未支付-监听者
     * @param info
     */
    @RabbitListener(queues = {"${mq.kill.item.success.kill.dead.real.queue}"},containerFactory = "singleListenerContainer")
    public void consumeExpireOrder(KillSuccessUserInfo info){
        try {
            log.info("用户秒杀成功后超时未支付-监听者-接收消息:{}",info);

            if (info!=null){
                ItemKillSuccess entity=itemKillSuccessMapper.selectByPrimaryKey(info.getCode());
                if (entity!=null && entity.getStatus().intValue()==0){
                    itemKillSuccessMapper.expireOrder(info.getCode());
                }
            }

        }catch (Exception e){
            log.error("用户秒杀成功后超时未支付-监听者-发生异常：",e.fillInStackTrace());
        }
    }
}
