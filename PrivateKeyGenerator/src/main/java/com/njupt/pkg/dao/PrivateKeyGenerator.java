package com.njupt.pkg.dao;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import java.nio.charset.StandardCharsets;

@Repository
public class PrivateKeyGenerator {
    private final Logger logger= LoggerFactory.getLogger(getClass());
    private Field<Element> G1;
    private Field<Element> Zr;
    private Element masterSecretKey;
    private Element groupGenerator;
    private Element masterPublicKey;

    public PrivateKeyGenerator(){
        //对应上文的pairing
        Pairing pairing = PairingFactory.getPairing("curve_a.properties");
        PairingFactory.getInstance().setUsePBCWhenPossible(true);
        //根据pairing生成对应Field
        this.Zr = pairing.getZr();
        this.G1 = pairing.getG1();
        this.masterSecretKey = Zr.newRandomElement().getImmutable();
        this.groupGenerator = G1.newElementFromHash("ArrayIndexOutOfBoundsException".getBytes(StandardCharsets.UTF_8), 0, 30).getImmutable();
        this.masterPublicKey = groupGenerator.mulZn(masterSecretKey);
        logger.info("PKG初始化完毕");
        logger.info("群生成元 :" + groupGenerator.toString());
        logger.info("主公钥  :" + masterPublicKey.toString());
    }

    public Element getMasterPublicKey(){
        return this.masterPublicKey;
    }

    public Element getUserPrivateKey(String userIdentity){
        byte[] userIdentityBytes = userIdentity.getBytes(StandardCharsets.UTF_8);
        Element zr = Zr.newElementFromHash(userIdentityBytes, 0, userIdentityBytes.length);
        return groupGenerator.mulZn(zr);
    }
}
