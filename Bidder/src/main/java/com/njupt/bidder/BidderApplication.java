package com.njupt.bidder;

import com.njupt.bidder.component.Bidder;
import org.hyperledger.fabric.client.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import java.io.IOException;



@SpringBootApplication
public class BidderApplication {
    public static void main(String[] args) throws IOException, EndorseException, CommitException, SubmitException, CommitStatusException, InterruptedException {
        ApplicationContext applicationContext = SpringApplication.run(BidderApplication.class, args);
        Bidder bidder = applicationContext.getBean(Bidder.class);
        Thread.sleep(3000);
        bidder.submitFirstRoundInput();
    }
}
