package com.fabric;

import com.fabric.sdk.*;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FabricInit {
    private static final Logger log = LoggerFactory.getLogger(FabricInit.class);
    public static void main(String[] args) {

    }

    public static void initChannel(String[] args) throws IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, org.hyperledger.fabric.sdk.exception.CryptoException, org.bouncycastle.crypto.CryptoException, InvalidArgumentException, InvocationTargetException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, TransactionException, ProposalException {
        UserContext userContext = new UserContext();
        userContext.setAffiliation("Org1");
        userContext.setMspId("Org1MSP");
        userContext.setAccount("李伟");
        userContext.setName("admin");
        Enrollment enrollment = UserUtils.getEnrollment(Config.keyFolderPath, Config.keyFileName, Config.certFoldePath, Config.certFileName);
        userContext.setEnrollment(enrollment);
        FabricClient fabricClient = new FabricClient(userContext);

//        Channel channel = fabricClient.getChannel("aaabbb");
//        channel.initialize();
//        BlockchainInfo blockchainInfo=channel.queryBlockchainInfo();
//        System.out.println(blockchainInfo.getHeight());

        Channel channel = fabricClient.getChannel("mychannel");
        Orderer orderer=fabricClient.getOrderer("orderer.example.com", "grpcs://orderer.example.com:7050", Config.tlsOrderFilePath);
        Peer peer=fabricClient.getPeer("peer0.org1.example.com", "grpcs://peer0.org1.example.com:7051", Config.tlsPeerFilePath);
        channel.addOrderer(orderer);
        channel.addPeer(peer);
        channel.initialize();
        BlockchainInfo blockchainInfo = channel.queryBlockchainInfo();
        System.out.println(blockchainInfo.getHeight());

        List<Peer> peers = new ArrayList<>();
        peers.add(peer);

        String initArgs[] = {"a"};
        QueryByChaincodeRequest request=QueryByChaincodeRequest.newInstance(userContext);
        request.setArgs(initArgs);
        request.setChaincodeID(ChaincodeID.newBuilder().setName("mckaytest1").build());
        request.setChaincodeLanguage(TransactionRequest.Type.JAVA);
        request.setFcn("query");
        Collection<ProposalResponse> responses= channel.queryByChaincode(request);
        System.out.println(responses);
        System.out.println("-------------------");
        responses.stream().forEach(res->{
//            System.out.println(res.getProposalResponse().getPayload().toString());
            System.out.println(res.getProposalResponse().getResponse().getPayload().toStringUtf8());
            System.out.println("-------------------");
        });


//        Map map =  fabricClient.queryChaincode(peers,"mychannel", TransactionRequest.Type.JAVA,"mycc","query",initArgs);
//        System.out.println(map);

//       Channel channel =  fabricClient.createChannel("test",fabricClient.getOrderer("orderer.example.com","grpcs://orderer.example.com:7050",tlsOrderFilePath),txfilePath);
    }
}
