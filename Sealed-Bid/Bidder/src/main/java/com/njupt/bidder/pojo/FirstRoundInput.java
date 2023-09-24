package com.njupt.bidder.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

/**
 * 第一轮输入参数类
 * identity: 用户身份ID，也是公钥
 * ciphers: 用户出价的加密密文，每一个二进制位对应一个byte[2][128]的数组
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirstRoundInput implements Serializable {
    private String identity;
    private List<byte[][]> ciphers;
    
}
