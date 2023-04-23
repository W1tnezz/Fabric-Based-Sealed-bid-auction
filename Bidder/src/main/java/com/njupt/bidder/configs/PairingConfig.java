package com.njupt.bidder.configs;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PairingConfig {
    @Bean
    public Pairing pairing(){
        Pairing pairing = PairingFactory.getPairing("curve_a.properties");
        PairingFactory.getInstance().setUsePBCWhenPossible(true);
        return pairing;
    }
    @Bean
    @SuppressWarnings("unchecked")
    public Field<Element> Zr(Pairing pairing){
        return pairing.getZr();
    }
    @Bean
    @SuppressWarnings("unchecked")
    public Field<Element> G1(Pairing pairing){
        return pairing.getG1();
    }
    @Bean
    @SuppressWarnings("unchecked")
    public Field<Element> GT(Pairing pairing){
        return pairing.getGT();
    }
}
