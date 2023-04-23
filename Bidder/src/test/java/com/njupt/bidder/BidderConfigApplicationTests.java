package com.njupt.bidder;

import com.njupt.bidder.component.Bidder;
import com.njupt.bidder.utils.PrivateKeyGeneratorConnector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BidderConfigApplicationTests {

    @Autowired
    private PrivateKeyGeneratorConnector pkgConnector;
    @Autowired
    private Bidder bidder;
    @Test
    void contextLoads() {
        System.out.println(pkgConnector.getMasterPublicKey());
        System.out.println(pkgConnector.getSecretKey(bidder.getIdentity()));
    }

}
