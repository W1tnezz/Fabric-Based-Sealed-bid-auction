package com.njupt.bidder;

import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.GatewayException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class BidderApplication {
    public static void main(String[] args) throws GatewayException, CommitException {
        ApplicationContext applicationContext = SpringApplication.run(BidderApplication.class, args);
        Contract contract = applicationContext.getBean(Contract.class);
        System.out.println(new String(contract.submitTransaction("queryAll")));

    }
}
