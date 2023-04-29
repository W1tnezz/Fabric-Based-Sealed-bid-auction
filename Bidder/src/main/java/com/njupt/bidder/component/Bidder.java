package com.njupt.bidder.component;

import com.njupt.bidder.pojo.FirstRoundInput;
import com.njupt.bidder.pojo.SecondRoundCipher4Someone;
import com.njupt.bidder.pojo.SecondRoundInput;
import com.njupt.bidder.utils.CryptoUtils;
import com.njupt.bidder.utils.SerializeUtils;
import it.unisa.dia.gas.jpbc.Element;
import lombok.Data;
import org.hyperledger.fabric.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Data
@Component
public class Bidder {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private String identity;
    private Element secretKey;
    private Element masterPublicKey;
    private final int BIDDER_NUM;
    private int bid;
    private String bidBinaryStr;
    private Contract contract;
    private List<FirstRoundInput> othersFirstRoundInput = new ArrayList<>();
    private List<SecondRoundInput> othersSecondRoundInput = new ArrayList<>();

    /**
     * Bidder初始化身份(公钥),主公钥,私钥,出价
     */
    @Autowired
    public Bidder(ApplicationArguments applicationArguments, PrivateKeyGeneratorConnector privateKeyGeneratorConnector, Contract contract) {
        this.identity = applicationArguments.getOptionValues("identity").get(0);
        this.BIDDER_NUM = Integer.parseInt(applicationArguments.getOptionValues("n").get(0));
        this.masterPublicKey = privateKeyGeneratorConnector.getMasterPublicKey().getImmutable();
        this.secretKey = privateKeyGeneratorConnector.getSecretKey(this.identity).getImmutable();
        Random random = new Random();
        this.bid = random.nextInt(Integer.MAX_VALUE);
        this.bidBinaryStr = String.format("%32s", Integer.toBinaryString(this.bid)).replace(" ", "0");
        this.contract = contract;
        logger.info("Bidder " + this.identity + " is ready, his bid is " + this.bid + "!");
    }

    public FirstRoundInput getFirstRoundInput() {
        List<byte[][]> input = new ArrayList<>();
        String bidStr = this.bidBinaryStr;
        logger.info(this.identity + "的出价二进制格式为：" + bidStr);
        for (int i = 0; i < bidStr.length(); i++) {
            Element[] cipher = CryptoUtils.encrypt(bidStr.charAt(i) - '0', this.masterPublicKey, identity);
            input.add(new byte[][]{cipher[0].toBytes(), cipher[1].toBytes()});
        }
        return new FirstRoundInput(this.identity, input);
    }

    public void submitFirstRoundInput() throws IOException, EndorseException, CommitException, SubmitException, CommitStatusException {
        byte[] input = SerializeUtils.firstRoundInput2Bytes(this.getFirstRoundInput());
        String inputStr = new String(input, StandardCharsets.ISO_8859_1);
        contract.newProposal("submitFirstRoundInput")
                .addArguments(this.identity, inputStr)
                .build()
                .endorse()
                .submit();
    }

    public void appendOthersFirstRoundInput(FirstRoundInput firstRoundInput) throws IOException, EndorseException, CommitException, SubmitException, CommitStatusException {
        logger.info("在链上收到来自" + firstRoundInput.getIdentity() + "的第一轮输入");
        this.othersFirstRoundInput.add(firstRoundInput);
        if (this.othersFirstRoundInput.size() == this.BIDDER_NUM - 1) {
            logger.info("收到所有其他参与者的输入，开始计算第二轮输入");
            this.submitSecondRoundInput();
        }
    }

    public void submitSecondRoundInput() throws IOException, EndorseException, CommitException, SubmitException, CommitStatusException {
        byte[] input = SerializeUtils.secondRoundInput2Bytes(calSecondRoundInput(), this.BIDDER_NUM - 1);
        String inputStr = new String(input, StandardCharsets.ISO_8859_1);
        contract.newProposal("submitSecondRoundInput")
                .addArguments(this.identity, inputStr)
                .build()
                .endorse()
                .submit();
    }

    public SecondRoundInput calSecondRoundInput() {
        SecondRoundInput secondRoundInput = new SecondRoundInput();
        secondRoundInput.setIdentity(this.identity);
        secondRoundInput.setCiphers(new ArrayList<>());
        for (FirstRoundInput firstRoundInput : this.othersFirstRoundInput) {
            List<byte[][]> ciphersByte = firstRoundInput.getCiphers();
            List<Element[]> ciphers = new ArrayList<>();
            for (byte[][] bytes : ciphersByte) {
                Element c1 = CryptoUtils.newG1ElementFromBytes(bytes[0]).getImmutable();
                Element c2 = CryptoUtils.newGTElementFromBytes(bytes[1]).getImmutable();
                ciphers.add(new Element[]{c1, c2});
            }

            SecondRoundCipher4Someone temp = new SecondRoundCipher4Someone();
            temp.setIdentity(firstRoundInput.getIdentity());
            List<byte[][]> ciphersDGK = new ArrayList<>();
            for (int i = 0; i < ciphers.size(); i++) {
                Element[] cipher = ciphers.get(i);
                //  zr = Vi + 1
                Element zr = CryptoUtils.getZr().newElement(this.bidBinaryStr.charAt(i) - '0' + 1);
                Element newC1 = CryptoUtils.getG1().newZeroElement();
                Element newC2 = CryptoUtils.getGtGenerator().powZn(zr);
                for (int j = i - 1; j >= 0; j--) {
                    // zrTemp = 1 - 2 * Vj
                    Element zrTemp = CryptoUtils.getZr().newElement(1 - 2 * (this.bidBinaryStr.charAt(j) - '0')).getImmutable();
                    // zrTemp1 = Vj
                    Element zrTemp1 = CryptoUtils.getZr().newElement(this.bidBinaryStr.charAt(j) - '0').getImmutable();

                    // g1Temp = (1 - 2 * Vj) * Cj(0)
                    Element g1Temp = ciphers.get(j)[0].mulZn(zrTemp);
                    // gtTemp = Cj(1) ^ (1 - 2 * Vj)
                    Element gtTemp = ciphers.get(j)[1].powZn(zrTemp);
                    Element gtTemp1 = CryptoUtils.getGtGenerator().powZn(zrTemp1);
                    newC1 = newC1.add(g1Temp);
                    newC2 = newC2.mul(gtTemp);
                    newC2 = newC2.mul(gtTemp1);
                }
                newC1 = newC1.add(cipher[0].negate());
                newC2 = newC2.mul(cipher[1].invert());
                ciphersDGK.add(new byte[][]{newC1.toBytes(), newC2.toBytes()});
            }
            temp.setCiphers(ciphersDGK);
            secondRoundInput.getCiphers().add(temp);
        }
        return secondRoundInput;
    }

    public void appendOthersSecondRoundInput(SecondRoundInput secondRoundInput) throws IOException, EndorseException, CommitException, SubmitException, CommitStatusException {
        logger.info("在链上收到来自" + secondRoundInput.getIdentity() + "的第二轮输入");
        this.othersSecondRoundInput.add(secondRoundInput);
        if (this.othersSecondRoundInput.size() == this.BIDDER_NUM - 1) {
            logger.info("收到所有其他参与者的输入，开始计算自己的排名");
            calRank();
        }
    }

    public void calRank() {
        int count = 0;
        for (SecondRoundInput secondRoundInput : othersSecondRoundInput) {
            for (SecondRoundCipher4Someone cipher : secondRoundInput.getCiphers()) {
                if (cipher.getIdentity().equals(this.identity)) {
                    for (byte[][] bytes : cipher.getCiphers()) {
                        Element c1 = CryptoUtils.newG1ElementFromBytes(bytes[0]);
                        Element c2 = CryptoUtils.newGTElementFromBytes(bytes[1]);
                        if (CryptoUtils.decrypt(new Element[]{c1, c2}, this.secretKey) != null){
                            count++;
                            break;
                        }
                    }
                    break;
                }
            }
        }
        System.out.println(count);
    }
}
