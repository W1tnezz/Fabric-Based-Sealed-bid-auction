package com.njupt.bidder.pojo;

import it.unisa.dia.gas.jpbc.Element;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirstRoundInput implements Serializable {
    private String identity;
    private List<Element[]> ciphers;
    
}
