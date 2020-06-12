package com.zzm.kill.model.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Item {
    private Integer id;

    private String name;

    private String code;

    private Long stock;

    private Date purchaseTime;

    private Integer isActive;

    private Date createTime;

    private Date updateTime;
}