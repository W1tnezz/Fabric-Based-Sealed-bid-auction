package com.njupt.bidder;

import com.njupt.bidder.component.Bidder;
import com.njupt.bidder.component.PrivateKeyGeneratorConnector;
import org.hyperledger.fabric.client.Network;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BidderConfigApplicationTests {

    @Autowired
    private PrivateKeyGeneratorConnector pkgConnector;
    @Autowired
    private Bidder bidder;
    @Autowired
    private Network network;
    @Test
    void contextLoads() {
        System.out.println(pkgConnector.getMasterPublicKey());
        System.out.println(pkgConnector.getSecretKey(bidder.getIdentity()));
        System.out.println(network.getContract("mycc").getChaincodeName());
    }

}
