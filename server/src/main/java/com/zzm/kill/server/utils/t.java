package com.zzm.kill.server.utils;

import org.apache.shiro.crypto.hash.Md5Hash;

/**
 * @author zzm
 * @data 2020/6/11 11:09
 */
public class t {
    public static void main(String[] args) {
        String newPsd=new Md5Hash("123456","flzx3qcysyhljt").toString();
        System.out.println(newPsd);
    }
}
