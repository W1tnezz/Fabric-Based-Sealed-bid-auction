package com.njupt.bidder.utils;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import java.nio.charset.StandardCharsets;

public class CryptoUtils {
    private static final Pairing pairing = PairingFactory.getPairing("curve_a.properties");

    @SuppressWarnings("unchecked")
    private static final Field<Element> Zr = pairing.getZr();

    @SuppressWarnings("unchecked")
    private static final Field<Element> G1 = pairing.getG1();

    @SuppressWarnings("unchecked")
    private static final Field<Element> GT = pairing.getGT();

    private static final Element groupGenerator = G1.newElementFromHash("ArrayIndexOutOfBoundsException".getBytes(StandardCharsets.UTF_8), 0, 30).getImmutable();

    private static final Element gtGenerator = pairing.pairing(groupGenerator, groupGenerator).getImmutable();

    public static Element getGroupGenerator(){
        return groupGenerator;
    }

    public static Element newG1ElementFromBytes(byte[] bytes){
        return G1.newElementFromBytes(bytes);
    }

    public static Element[] encrypt(int m, Element masterPublicKey, String identity){
        Element r = Zr.newRandomElement().getImmutable();
        Element mZr = Zr.newElement(m);
        Element c1 = groupGenerator.mulZn(r);
        Element c2 = gtGenerator.powZn(mZr);
        byte[] id = identity.getBytes(StandardCharsets.UTF_8);
        Element idHash = G1.newElementFromHash(id, 0, id.length);
        Element temp = pairing.pairing(idHash, masterPublicKey.powZn(r));
        c2 = c2.mul(temp);
        return new Element[]{c1, c2};
    }

    public static Integer decrypt(Element[] c, Element secretKey){
        Element temp = pairing.pairing(c[0], secretKey);
        Element res = c[1].mul(temp.invert());
        if(res.isEqual(GT.newZeroElement())){
            return 0;
        }
        return null;
    }
}
