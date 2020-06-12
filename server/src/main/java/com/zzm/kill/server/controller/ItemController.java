package com.zzm.kill.server.controller;

import com.zzm.kill.model.entity.ItemKill;
import com.zzm.kill.model.mapper.ItemKillMapper;
import com.zzm.kill.server.service.impl.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;


/**
 * 待秒杀商品controller
 *
 * @author zzm
 * @data 2020/6/8 8:24
 */
@Controller
public class ItemController {

    private static final Logger log = LoggerFactory.getLogger(ItemController.class);

    private static final String prefix = "item";

    @Autowired
    private ItemService itemService;

    @GetMapping(value = {"/", "/index", prefix + "/list", prefix + "/index.html"})
    public String getList(ModelMap modelMap) {
        try {
            List<ItemKill> list = itemService.getKillItems();
            modelMap.put("list",list);
            log.info("获取待秒杀商品列表-数据：{}",list);
        }catch (Exception e){
            log.error("获取待秒杀商品列表-发生异常：",e.fillInStackTrace());
            return "redirect:/base/error";
        }
        return "list";
    }

    /**
     * 获取待秒杀商品的详情
     * @return
     */
    @GetMapping(value = prefix+"/detail/{id}")
    public String detail(@PathVariable Integer id, ModelMap modelMap){
        if (id==null || id<=0){
            return "redirect:/base/error";
        }
        try {
            ItemKill detail = itemService.getKillDetail(id);
            modelMap.put("detail",detail);
        }catch (Exception e){
            log.error("获取待秒杀商品的详情-发生异常：id={}",id,e.fillInStackTrace());
            return "redirect:/base/error";
        }
        return "info";
    }
}
