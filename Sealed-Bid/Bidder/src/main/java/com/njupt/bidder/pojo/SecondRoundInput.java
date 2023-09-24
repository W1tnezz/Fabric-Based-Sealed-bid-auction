package com.njupt.bidder.pojo;

import it.unisa.dia.gas.jpbc.Element;
import lombok.Data;

import java.util.List;

@Data
public class SecondRoundInput {
    private String identity;
    private List<SecondRoundCipher4Someone> ciphers;

}
