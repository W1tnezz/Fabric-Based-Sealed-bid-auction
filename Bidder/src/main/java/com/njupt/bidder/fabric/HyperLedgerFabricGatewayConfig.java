package com.njupt.bidder.fabric;

import com.njupt.bidder.component.Bidder;
import com.njupt.bidder.utils.YmlUtils;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.client.CallOption;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * @author xu zheqing
 * @date 2023/4/21
 */
@Configuration
@AllArgsConstructor
@Slf4j
public class HyperLedgerFabricGatewayConfig {
    private HyperLedgerFabricProperties hyperLedgerFabricProperties;
    @Autowired
    public HyperLedgerFabricGatewayConfig(ApplicationArguments applicationArguments) throws FileNotFoundException {
        this.hyperLedgerFabricProperties = YmlUtils.toObject(applicationArguments.getOptionValues("filePath").get(0), HyperLedgerFabricProperties.class);
    }
    @Bean
    public Gateway gateway() throws Exception {

        BufferedReader certificateReader = Files.newBufferedReader(Paths.get(hyperLedgerFabricProperties.getCertificatePath()), StandardCharsets.UTF_8);
        X509Certificate certificate = Identities.readX509Certificate(certificateReader);
        BufferedReader privateKeyReader = Files.newBufferedReader(Paths.get(hyperLedgerFabricProperties.getPrivateKeyPath()), StandardCharsets.UTF_8);
        PrivateKey privateKey = Identities.readPrivateKey(privateKeyReader);

        Gateway gateway = Gateway.newInstance()
                .identity(new X509Identity(hyperLedgerFabricProperties.getMspId() , certificate))
                .signer(Signers.newPrivateKeySigner(privateKey))
                .connection(newGrpcConnection())
                .evaluateOptions(CallOption.deadlineAfter(5, TimeUnit.SECONDS))
                .endorseOptions(CallOption.deadlineAfter(15, TimeUnit.SECONDS))
                .submitOptions(CallOption.deadlineAfter(5, TimeUnit.SECONDS))
                .commitStatusOptions(CallOption.deadlineAfter(1, TimeUnit.MINUTES))
                .connect();

        log.info("=========================================== connected fabric gateway {} " , gateway);
        return gateway;
    }
    private ManagedChannel newGrpcConnection() throws IOException, CertificateException {
        Reader tlsCertReader = Files.newBufferedReader(Paths.get(hyperLedgerFabricProperties.getTlsCertPath()));
        X509Certificate tlsCert = Identities.readX509Certificate(tlsCertReader);
        if(hyperLedgerFabricProperties.getMspId().equals("Org1MSP")){
            return NettyChannelBuilder.forTarget("peer0.org1.example.com:7051")
                    .sslContext(GrpcSslContexts.forClient().trustManager(tlsCert).build())
                    .overrideAuthority("peer0.org1.example.com")
                    .build();
        }else {
            return NettyChannelBuilder.forTarget("peer0.org2.example.com:8051")
                    .sslContext(GrpcSslContexts.forClient().trustManager(tlsCert).build())
                    .overrideAuthority("peer0.org2.example.com")
                    .build();
        }

    }
    @Bean
    public Network network(Gateway gateway) {
        return gateway.getNetwork(hyperLedgerFabricProperties.getChannel());
    }
    @Bean
    public Contract bidContract(Network network) {
        return network.getContract("mycc");
    }
    @Bean
    public ChaincodeEventListener chaincodeEventListener(Network network, Bidder bidder) {
        return new ChaincodeEventListener(network, bidder);
    }
}
