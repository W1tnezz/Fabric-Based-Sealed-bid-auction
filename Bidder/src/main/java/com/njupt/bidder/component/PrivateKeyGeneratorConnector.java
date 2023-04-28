package com.njupt.bidder.component;

import com.njupt.bidder.utils.CryptoUtils;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PrivateKeyGeneratorConnector {
    @Value("${publicKeyGenerator.url}")
    private String pkgUrl;
    @Autowired
    private RestTemplate restTemplate;

    public Element getMasterPublicKey(){
        String request = "getMPK";
        byte[] result = restTemplate.getForObject(pkgUrl + "/{request}", byte[].class, request);
        return CryptoUtils.newG1ElementFromBytes(result);
    }
    public Element getSecretKey(String identity){
        String request = "getSK";
        byte[] result = restTemplate.getForObject(pkgUrl + "/{request}/{identity}", byte[].class, request, identity);
        return CryptoUtils.newG1ElementFromBytes(result);
    }

}
