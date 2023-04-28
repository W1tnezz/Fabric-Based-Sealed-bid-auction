package com.njupt.bidder.pojo;

import it.unisa.dia.gas.jpbc.Element;
import lombok.Data;

import java.util.List;

@Data
public class ThirdRoundInput {
    private String identity;
    private List<Element[]> ciphers;
    private List<Element> tokens;
}
