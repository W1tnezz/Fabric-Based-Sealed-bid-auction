package com.njupt.bidder.component;

import it.unisa.dia.gas.jpbc.Element;
import org.springframework.stereotype.Component;

@Component
public class Bidder {
    private final String identity;
    private Element secretKey;
    private int bid;

    public Bidder(){
        this.identity = "I am Java";
    }
    public Bidder(String identity){
        this.identity = identity;
    }
    public String getIdentity() {
        return identity;
    }

    public Element getSecretKey() {
        return secretKey;
    }

    public int getBid() {
        return bid;
    }


}
