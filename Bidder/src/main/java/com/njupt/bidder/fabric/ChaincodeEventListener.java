package com.njupt.bidder.fabric;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.StringUtils;
import org.hyperledger.fabric.client.ChaincodeEvent;
import org.hyperledger.fabric.client.CloseableIterator;
import org.hyperledger.fabric.client.Network;


/**
 * @author xu zheqing
 * @date 2023/4/27
 */

@Slf4j
public class ChaincodeEventListener implements Runnable {


    final Network network;

    public ChaincodeEventListener(Network network) {
        this.network = network;

//        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
//            @Override
//            public Thread newThread(@NonNull Runnable r) {
//                Thread thread = new Thread(r);
//                // thread.setDaemon(true);
//                thread.setName(this.getClass() + "chaincode_event_listener");
//                return thread;
//            }
//        });
//        executor.schedule(this, 0, TimeUnit.SECONDS);

        Thread thread = new Thread(this);
        // thread.setDaemon(true);
        thread.setName(this.getClass() + "chaincode_event_listener");
        thread.start();
    }

    @Override
    public void run() {
        CloseableIterator<ChaincodeEvent> events = network.getChaincodeEvents("mycc");
        log.info("chaincodeEvents {} " , events);
        // events.hasNext() 会阻塞等待
        while (events.hasNext()) {
            ChaincodeEvent event = events.next();
            log.info("receive chaincode event {} , transaction id {} ,  block number {} , payload {} "
                    , event.getEventName() , event.getTransactionId() , event.getBlockNumber() , StringUtils.newStringUtf8(event.getPayload()));

        }
    }
}

