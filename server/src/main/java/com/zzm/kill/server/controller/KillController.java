package com.zzm.kill.server.controller;

import com.zzm.kill.api.enums.StatusCode;
import com.zzm.kill.api.response.BaseResponse;
import com.zzm.kill.model.dto.KillSuccessUserInfo;
import com.zzm.kill.model.mapper.ItemKillSuccessMapper;
import com.zzm.kill.server.annotation.RateLimter;
import com.zzm.kill.server.dto.KillDto;
import com.zzm.kill.server.service.IKillService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

/**
 * 秒杀controller
 *
 * @author zzm
 * @data 2020/6/8 8:53
 */
@Controller
public class KillController {

    private static final Logger log = LoggerFactory.getLogger(KillController.class);

    private static final String prefix = "kill";

    @Autowired
    private IKillService killService;

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    @PostMapping(value = prefix + "/execute", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public BaseResponse excute(@RequestBody @Validated KillDto dto, BindingResult result, HttpSession session) {
        if (result.hasErrors() || dto.getKillId() <= 0) {
            return new BaseResponse(StatusCode.InvalidParams);
        }
        Object uId=session.getAttribute("uid");
//        Object uId = dto.getUserId();
        if (uId == null) {
            return new BaseResponse(StatusCode.UserNotLogin);
        }
        Integer userId = (Integer) uId;
        BaseResponse response = new BaseResponse(StatusCode.Success);
        try {
            Boolean res = killService.killItem(dto.getKillId(), userId);
            if (!res) {
                return new BaseResponse(StatusCode.Fail.getCode(), "哈哈~商品已抢购完毕或者不在抢购时间段哦!");
            }
        } catch (Exception e) {
            response = new BaseResponse(StatusCode.Fail.getCode(), e.getMessage());
        }
        return response;
    }

    /***
     * 商品秒杀核心业务逻辑-用于压力测试
     * @param dto
     * @param result
     * @return
     */
    @RequestMapping(value = prefix + "/execute/lock", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @RateLimter
    public BaseResponse executeLock(@RequestBody @Validated KillDto dto, BindingResult result, HttpSession session) {
        if (result.hasErrors() || dto.getKillId() <= 0) {
            return new BaseResponse(StatusCode.InvalidParams);
        }
        Object uId=session.getAttribute("uid");
//        Object uId = dto.getUserId();
        if (uId == null) {
            return new BaseResponse(StatusCode.UserNotLogin);
        }
        Integer userId = (Integer) uId;
        BaseResponse response = new BaseResponse(StatusCode.Success);
        try {
            //不加分布式锁的前提  最后减库存时再确认库存大于1时更改
//            Boolean res=killService.killItemV2(dto.getKillId(),dto.getUserId());
//            if (!res){
//                return new BaseResponse(StatusCode.Fail.getCode(),"不加分布式锁-哈哈~商品已抢购完毕或者不在抢购时间段哦!");
//            }

            //基于Redis的分布式锁进行控制
//            Boolean res=killService.killItemV3(dto.getKillId(),dto.getUserId());
//            if (!res){
//                return new BaseResponse(StatusCode.Fail.getCode(),"基于Redis的分布式锁进行控制-哈哈~商品已抢购完毕或者不在抢购时间段哦!");
//            }

//            基于Redisson的分布式锁进行控制
            Boolean res = killService.killItemV4(dto.getKillId(), userId);
            if (!res) {
                return new BaseResponse(StatusCode.Fail.getCode(), "基于Redisson的分布式锁进行控制-哈哈~商品已抢购完毕或者不在抢购时间段哦!");
            }

            //基于ZooKeeper的分布式锁进行控制
//            Boolean res=killService.killItemV5(dto.getKillId(),dto.getUserId());
//            if (!res){
//                return new BaseResponse(StatusCode.Fail.getCode(),"基于ZooKeeper的分布式锁进行控制-哈哈~商品已抢购完毕或者不在抢购时间段哦!");
//            }

        } catch (Exception e) {
            response = new BaseResponse(StatusCode.Fail.getCode(), e.getMessage());
        }
        return response;
    }

    //抢购成功跳转页面
    @GetMapping(value = prefix + "/execute/success")
    public String executeSuccess() {
        return "executeSuccess";
    }

    //抢购失败跳转页面
    @GetMapping(value = prefix + "/execute/fail")
    public String executeFail() {
        return "executeFail";
    }

    /**
     * 查看订单详情
     *
     * @return
     */
    @GetMapping(value = prefix + "/record/detail/{orderNo}")
    public String killRecordDetail(@PathVariable String orderNo, ModelMap modelMap) {
        if (StringUtils.isBlank(orderNo)) {
            return "error";
        }
        KillSuccessUserInfo info = itemKillSuccessMapper.selectByCode(orderNo);
        if (info == null) {
            return "error";
        }
        modelMap.put("info", info);
        return "killRecord";
    }



}
