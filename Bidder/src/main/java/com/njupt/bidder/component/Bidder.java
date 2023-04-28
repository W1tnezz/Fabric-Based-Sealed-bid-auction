package com.njupt.bidder.component;

import com.njupt.bidder.pojo.FirstRoundInput;
import com.njupt.bidder.utils.CryptoUtils;
import it.unisa.dia.gas.jpbc.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class Bidder {
    private final Logger logger= LoggerFactory.getLogger(getClass());
    private String identity;
    private Element secretKey;
    private Element masterPublicKey;
    private int bid;

    /**
    * Bidder初始化身份(公钥),主公钥,私钥,出价
    * */

    @Autowired
    public Bidder(ApplicationArguments applicationArguments, PrivateKeyGeneratorConnector privateKeyGeneratorConnector){
        this.identity = applicationArguments.getOptionValues("identity").get(0);
        this.masterPublicKey = privateKeyGeneratorConnector.getMasterPublicKey().getImmutable();
        this.secretKey = privateKeyGeneratorConnector.getSecretKey(this.identity).getImmutable();
        Random random = new Random();
        this.bid = random.nextInt();
        logger.info("Bidder " + this.identity + " is ready, his bid is " + this.bid + "!");
    }


    public String getIdentity() {
        return identity;
    }
    public int getBid() {
        return bid;
    }

    public FirstRoundInput getFirstRoundInput(){
        List<Element[]> input = new ArrayList<>();
        String bidStr = String.format("%32s", Integer.toBinaryString(this.bid)).replace(" ","0");
        logger.info(this.identity + "的出价二进制格式为：" + bidStr);
        for (int i = 0; i < bidStr.length(); i++) {
            Element[] cipher = CryptoUtils.encrypt(bidStr.charAt(i) - '0', this.masterPublicKey, identity);
            input.add(cipher);
        }
        return new FirstRoundInput(this.identity, input);
    }




}
