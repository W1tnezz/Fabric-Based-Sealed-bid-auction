package com.njupt.bidder.fabric;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


/**
 * @author xu zheqing
 * @date 2023/4/21
 */
@Configuration
@Data
public class HyperLedgerFabricProperties {
    String mspId;
    String certificatePath;
    String privateKeyPath;
    String tlsCertPath;
    String channel;
}
