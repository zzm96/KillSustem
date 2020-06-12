package com.zzm.kill.api.response;

import com.zzm.kill.api.enums.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author zzm
 * @data 2020/6/7 15:54
 */
@Data
@AllArgsConstructor
public class BaseResponse <T>{
    private Integer code;
    private String msg;
    private T data;

    public BaseResponse(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public BaseResponse(StatusCode statusCode) {
        this.code = statusCode.getCode();
        this.msg = statusCode.getMsg();
    }


}
