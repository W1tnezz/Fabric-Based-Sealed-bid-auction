package com.njupt.bidder.pojo;

import lombok.Data;

import java.util.List;

/**
 * 第三轮输入，和身份为identity的拍卖者的密文比较结果，
 * proof中键为密文，值为对应的解密TOKEN
 * */
@Data
public class ThirdRoundCompareRes {
    private String identity;
    private List<byte[][]> ciphers;
    private List<byte[]> tokens;
}
