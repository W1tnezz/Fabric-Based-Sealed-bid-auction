package com.njupt.bidder.pojo;

import it.unisa.dia.gas.jpbc.Element;
import lombok.Data;

import java.util.List;

@Data
public class SecondRoundCipher4Someone {
    private String identity;
    private List<byte[][]> ciphers;
}
