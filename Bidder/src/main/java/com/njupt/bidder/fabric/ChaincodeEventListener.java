package com.njupt.bidder.fabric;

import com.njupt.bidder.component.Bidder;
import com.njupt.bidder.pojo.FirstRoundInput;
import com.njupt.bidder.pojo.SecondRoundInput;
import com.njupt.bidder.pojo.ThirdRoundInput;
import com.njupt.bidder.utils.SerializeUtils;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.client.*;


@Slf4j
public class ChaincodeEventListener implements Runnable {
    final Network network;
    final Bidder bidder;

    public ChaincodeEventListener(Network network, Bidder bidder) {
        this.network = network;
        this.bidder = bidder;
        Thread thread = new Thread(this);
        // thread.setDaemon(true);
        thread.setName(this.getClass() + "chaincode_event_listener");
        thread.start();
    }

    @Override
    public void run() {
        CloseableIterator<ChaincodeEvent> events = network.getChaincodeEvents("mycc");
        log.info("启动链码事件监听器： {} " , events);
        // events.hasNext() 会阻塞等待
        while (events.hasNext()) {
            ChaincodeEvent event = events.next();
            byte[] payLoad = event.getPayload();
            if(event.getEventName().equals("newFirstRoundInput")){
                try {
                    FirstRoundInput receivedCipher = SerializeUtils.Bytes2FirstRoundInput(payLoad);
                    if(!receivedCipher.getIdentity().equals(bidder.getIdentity())){
                        bidder.appendOthersFirstRoundInput(receivedCipher);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            if(event.getEventName().equals("newSecondRoundInput")){
                try {
                    SecondRoundInput receivedCipher = SerializeUtils.Bytes2SecondRoundInput(payLoad);
                    if(!receivedCipher.getIdentity().equals(bidder.getIdentity())){
                        bidder.appendOthersSecondRoundInput(receivedCipher);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            if(event.getEventName().equals("newThirdRoundInput")){
                try {
                    ThirdRoundInput receivedCipher = SerializeUtils.Bytes2ThirdRoundInput(payLoad);
                    if(!receivedCipher.getIdentity().equals(bidder.getIdentity())){
                        bidder.appendOthersThirdRoundInput(receivedCipher);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}

