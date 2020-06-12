package com.zzm.kill.server.service;

import com.zzm.kill.server.MainApplication;
import com.zzm.kill.server.dto.MailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

/**
 * 邮件服务
 * @author zzm
 * @data 2020/6/10 18:27
 */
@Service
@EnableAsync//允许使用多线程异步调用
public class MailService {
    private static final Logger log= LoggerFactory.getLogger(MailService.class);

    @Autowired
    Environment env;

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendSimpleEmail(MailDto mailDto){
        try {
            SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
            simpleMailMessage.setFrom(env.getProperty("mail.send.from"));
            simpleMailMessage.setTo(mailDto.getTos());
            simpleMailMessage.setSubject(mailDto.getSubject());
            simpleMailMessage.setText(mailDto.getContent());
            mailSender.send(simpleMailMessage);

        }catch (Exception e){
            log.error("发送简单文本文件-发生异常： ",e.fillInStackTrace());
        }
    }

    /**
     * 发送带有格式的邮件
     * @param dto
     */
    @Async
    public void sendHTMLMail(final MailDto dto){
        try {
            MimeMessage message=mailSender.createMimeMessage();
            MimeMessageHelper messageHelper=new MimeMessageHelper(message,true,"utf-8");
            messageHelper.setFrom(env.getProperty("mail.send.from"));
            messageHelper.setTo(dto.getTos());
            messageHelper.setSubject(dto.getSubject());
            messageHelper.setText(dto.getContent(),true);
            mailSender.send(message);
            log.info("发送花哨邮件-发送成功!");
        }catch (Exception e){
            log.error("发送花哨邮件-发生异常： ",e.fillInStackTrace());
        }
    }
}
