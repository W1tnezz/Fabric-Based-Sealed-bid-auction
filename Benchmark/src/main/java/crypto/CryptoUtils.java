package crypto;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
    public static Element newGTElementFromBytes(byte[] bytes) {
        return GT.newElementFromBytes(bytes);
    }

    public static Field<Element> getZr(){
        return Zr;
    }
    public static Field<Element> getG1(){
        return G1;
    }
    public static Field<Element> getGT(){
        return GT;
    }

    public static Element getGtGenerator(){
        return gtGenerator;
    }

    public static Element[] encrypt(int m, Element publicKey){
        Element r = Zr.newRandomElement().getImmutable();
        Element mZr = Zr.newElement(m);
        Element c1 = groupGenerator.mulZn(r);
        Element c2 = publicKey.mulZn(r);
        Element temp = groupGenerator.mulZn(mZr);
        c2 = c2.add(temp);
        return new Element[]{c1, c2};
    }

    public static Integer decrypt(Element[] c, Element secretKey){
        Element temp = c[0].mulZn(secretKey);
        Element res = c[1].add(temp.negate());
        if(res.isEqual(G1.newZeroElement())){
            return 0;
        }
        return null;
    }

    public static List<Element[]> DGK(List<Element[]> ciphers){
        Random r = new Random();
        String BinaryStr = String.format("%32s", Integer.toBinaryString(r.nextInt())).replace(" ", "0");
        List<Element[]> res = new ArrayList<>();
        for (int i = 0; i < ciphers.size(); i++) {
            Element[] cipher = ciphers.get(i);
            //  zr = Vi + 1
            Element zr = CryptoUtils.getZr().newElement(BinaryStr.charAt(i) - '0' + 1);
            Element newC1 = getG1().newZeroElement();
            Element newC2 = groupGenerator.mulZn(zr);
            for (int j = i - 1; j >= 0; j--) {
                // zrTemp = 1 - 2 * Vj
                Element zrTemp = CryptoUtils.getZr().newElement(1 - 2 * (BinaryStr.charAt(j) - '0')).getImmutable();
                // zrTemp1 = Vj
                Element zrTemp1 = CryptoUtils.getZr().newElement(BinaryStr.charAt(j) - '0').getImmutable();
                // g1Temp = (1 - 2 * Vj) * Cj(0)
                Element g1Temp = ciphers.get(j)[0].mulZn(zrTemp);
                // gtTemp = Cj(1) ^ (1 - 2 * Vj)
                Element gtTemp = ciphers.get(j)[1].mulZn(zrTemp);
                Element gtTemp1 = groupGenerator.mulZn(zrTemp1);
                newC1 = newC1.add(g1Temp);
                newC2 = newC2.add(gtTemp);
                newC2 = newC2.add(gtTemp1);
            }
            newC1 = newC1.add(cipher[0].negate());
            newC2 = newC2.add(cipher[1].negate());
            // 盲化
            Element blinder = CryptoUtils.getZr().newRandomElement().getImmutable();
            newC1 = newC1.mulZn(blinder);
            newC2 = newC2.mulZn(blinder);
            res.add(new Element[]{newC1, newC2});
        }
        Collections.shuffle(res);
        return res;
    }

    public static void calTokens(List<List<Element[]>> ciphersS, Element secretKey){
        for (int i = 0; i < ciphersS.size(); i++) {
            List<Element[]> ciphers = ciphersS.get(i);
            Collections.shuffle(ciphers);
            for (int j = 0; j < ciphers.size(); j++) {
                Element[] cipher = ciphers.get(j);
                Element blinder = CryptoUtils.getZr().newRandomElement();
                Element c1 = cipher[0];
                Element c2 = cipher[1];
                c1 = c1.mulZn(blinder);
                c2 = c2.mulZn(blinder);
                Element token = c1.mulZn(secretKey);
            }
        }
        Collections.shuffle(ciphersS);
    }
}
