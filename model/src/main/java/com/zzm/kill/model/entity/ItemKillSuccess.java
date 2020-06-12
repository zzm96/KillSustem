package com.zzm.kill.model.entity;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class ItemKillSuccess {
    private String code;

    private Integer itemId;

    private Integer killId;

    private String userId;

    private Byte status;

    private Date createTime;

    private Integer diffTime;

}