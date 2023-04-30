package com.njupt.bidder.component;

import com.njupt.bidder.pojo.*;
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
import java.util.*;


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
    private int RANK;
    private Contract contract;
    private List<FirstRoundInput> othersFirstRoundInput = new ArrayList<>();
    private List<SecondRoundInput> othersSecondRoundInput = new ArrayList<>();
    private List<ThirdRoundCompareRes> thirdRoundCompareRes = new ArrayList<>();
    private List<ThirdRoundInput> othersThirdRoundInput = new ArrayList<>();

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

    public synchronized void startAuction() throws EndorseException, CommitException, SubmitException, CommitStatusException, IOException, InterruptedException {
        logger.info("开始拍卖交互");
        this.submitFirstRoundInput();
        wait();
        this.submitSecondRoundInput();
        wait();
        this.calRank();
        // TODO: 第三轮交互，再次混洗所有的比较密文，并计算TOKEN
        this.submitThirdRoundInput();
        wait();
        this.verifyRank();
        logger.info("拍卖成功结束！");
    }

    /**
     * 提交第一轮输入，将序列化后的字节数组使用ISO_8859_1编码方式编码为字符串格式，
     * 目的是实现用字符串格式保存并传输字节数组，因为Fabric提供的链码调用接口只能传输字符串；
     */
    public void submitFirstRoundInput() throws IOException, EndorseException, CommitException, SubmitException, CommitStatusException {
        logger.info(identity + "开始计算自己的第一轮输入并提交到Fabric上");
        byte[] input = SerializeUtils.firstRoundInput2Bytes(this.getFirstRoundInput());
        String inputStr = new String(input, StandardCharsets.ISO_8859_1);
        contract.newProposal("submitFirstRoundInput")
                .addArguments(this.identity, inputStr)
                .build()
                .endorse()
                .submit();
    }

    /**
     * 根据自己的出价，分别加密每一个二进制位，并由一个Java对象保存；
     * */
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

    public synchronized void appendOthersFirstRoundInput(FirstRoundInput firstRoundInput) {
        logger.info("在链上收到来自" + firstRoundInput.getIdentity() + "的第一轮输入");
        this.othersFirstRoundInput.add(firstRoundInput);
        if (this.othersFirstRoundInput.size() == this.BIDDER_NUM - 1) {
            logger.info("收到所有其他参与者的输入，开始计算第二轮输入");
            notify();
        }
    }

    public void submitSecondRoundInput() throws IOException, EndorseException, CommitException, SubmitException, CommitStatusException {
        logger.info(identity + "开始计算自己的第二轮输入并提交到Fabric上");
        byte[] input = SerializeUtils.secondRoundInput2Bytes(calSecondRoundInput(), this.BIDDER_NUM - 1);
        String inputStr = new String(input, StandardCharsets.ISO_8859_1);
        contract.newProposal("submitSecondRoundInput")
                .addArguments(this.identity, inputStr)
                .build()
                .endorse()
                .submit();
    }

    private SecondRoundInput calSecondRoundInput() {
        SecondRoundInput secondRoundInput = new SecondRoundInput();
        secondRoundInput.setIdentity(this.identity);
        secondRoundInput.setCiphers(new ArrayList<>());
        for (FirstRoundInput firstRoundInput : this.othersFirstRoundInput) {
            logger.info("计算与" + firstRoundInput.getIdentity() + "的DGK比较密文...");
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

                // 盲化
                Element blinder = CryptoUtils.getZr().newRandomElement().getImmutable();
                newC1 = newC1.mulZn(blinder);
                newC2 = newC2.powZn(blinder);

                ciphersDGK.add(new byte[][]{newC1.toBytes(), newC2.toBytes()});
            }
            // 混洗
            Collections.shuffle(ciphersDGK);

            temp.setCiphers(ciphersDGK);
            secondRoundInput.getCiphers().add(temp);
        }
        return secondRoundInput;
    }

    public synchronized void appendOthersSecondRoundInput(SecondRoundInput secondRoundInput) throws IOException, EndorseException, CommitException, SubmitException, CommitStatusException {
        logger.info("在链上收到来自" + secondRoundInput.getIdentity() + "的第二轮输入");
        this.othersSecondRoundInput.add(secondRoundInput);
        if (this.othersSecondRoundInput.size() == this.BIDDER_NUM - 1) {
            logger.info("收到所有其他参与者的输入，开始计算自己的排名");
            notify();
        }
    }

    public void calRank() {
        int count = 0;
        for (SecondRoundInput secondRoundInput : othersSecondRoundInput) {
            for (SecondRoundCipher4Someone cipher : secondRoundInput.getCiphers()) {
                if (cipher.getIdentity().equals(this.identity)) {
                    ThirdRoundCompareRes temp = new ThirdRoundCompareRes();
                    List<byte[][]> list = new ArrayList<>();
                    temp.setIdentity(secondRoundInput.getIdentity());
                    temp.setCiphers(list);
                    temp.setTokens(new ArrayList<>());
                    boolean flag = true;
                    for (byte[][] bytes : cipher.getCiphers()) {
                        list.add(bytes);
                        if(flag){
                            Element c1 = CryptoUtils.newG1ElementFromBytes(bytes[0]);
                            Element c2 = CryptoUtils.newGTElementFromBytes(bytes[1]);
                            if (CryptoUtils.decrypt(new Element[]{c1, c2}, this.secretKey) != null){
                                count++;
                                flag = false;
                            }
                        }
                    }
                    this.thirdRoundCompareRes.add(temp);
                    break;
                }
            }
        }
        logger.info(identity + "在收到的第二轮消息中解密和自己相关的比较密文，其中有" + count + "组密文可以解密出0！");
        this.RANK = BIDDER_NUM - count;;
        logger.info(identity + "判断得知自己出价的排名为第" + RANK + "位！");
    }

    public void submitThirdRoundInput() throws IOException, EndorseException, CommitException, SubmitException, CommitStatusException {
        logger.info(identity + "开始计算自己的第三轮输入并提交到Fabric上");
        byte[] input = SerializeUtils.thirdRoundInput2Bytes(calThirdRoundInput(), this.BIDDER_NUM - 1);
        String inputStr = new String(input, StandardCharsets.ISO_8859_1);
        contract.newProposal("submitThirdRoundInput")
                .addArguments(this.identity, inputStr)
                .build()
                .endorse()
                .submit();
    }

    /**
     * List<ThirdRoundCompareRes> this.thirdRoundCompareRes:
     *      ThirdRoundCompareRes compareRes:
     *           String identity; //和我比较的bidder身份
     *           List<byte[][]> ciphers; // 比较密文，计算排名时添加到这里，在这里需要混洗和盲化；
     *           List<byte[]> tokens; // 解密token，混洗和盲化之后进行计算；
     * */
    private ThirdRoundInput calThirdRoundInput() {
        ThirdRoundInput res = new ThirdRoundInput();
        res.setIdentity(identity);
        res.setRank(this.RANK);
        res.setAllProofs(new ArrayList<>());
        for (ThirdRoundCompareRes compareRes : this.thirdRoundCompareRes) {
            Collections.shuffle(compareRes.getCiphers());
            for (int i = 0; i < compareRes.getCiphers().size(); i++) {
                byte[][] cipher = compareRes.getCiphers().get(i);
                Element blinder = CryptoUtils.getZr().newRandomElement();
                Element c1 = CryptoUtils.newG1ElementFromBytes(cipher[0]);
                Element c2 = CryptoUtils.newGTElementFromBytes(cipher[1]);
                c1 = c1.mulZn(blinder);
                c2 = c2.powZn(blinder);
                compareRes.getCiphers().set(i, new byte[][]{c1.toBytes(), c2.toBytes()});

                Element token = CryptoUtils.calPairing(c1, secretKey);
                compareRes.getTokens().add(token.toBytes());
            }
            res.getAllProofs().add(compareRes);
        }
        return res;
    }

    public synchronized void appendOthersThirdRoundInput(ThirdRoundInput thirdRoundInput) {
        logger.info("在链上收到来自" + thirdRoundInput.getIdentity() + "的第三轮输入");
        this.othersThirdRoundInput.add(thirdRoundInput);
        if (this.othersThirdRoundInput.size() == this.BIDDER_NUM - 1) {
            logger.info("收到所有其他参与者的第三轮输入，开始验证他们的排名");
            notify();
        }
    }

    public void verifyRank(){
        for (ThirdRoundInput thirdRoundInput : this.othersThirdRoundInput) {
            logger.info(thirdRoundInput.getIdentity() + "声称其排名为第" + thirdRoundInput.getRank() + ".");
            logger.info("开始验证他的排名...");
            int zeroNum = this.BIDDER_NUM - thirdRoundInput.getRank();
            List<ThirdRoundCompareRes> allProofs = thirdRoundInput.getAllProofs();
            int count = 0;
            for (ThirdRoundCompareRes proof : allProofs) {
                for (int i = 0; i < proof.getCiphers().size(); i++) {
                    byte[][] cipherBytes = proof.getCiphers().get(i);
                    byte[] tokenBytes = proof.getTokens().get(i);
                    Element c2 = CryptoUtils.newGTElementFromBytes(cipherBytes[1]);
                    Element token = CryptoUtils.newGTElementFromBytes(tokenBytes);
                    if(c2.mul(token.invert()).isEqual(CryptoUtils.getGT().newOneElement())){
                        count++;
                    }
                }
            }
            if(count == zeroNum){
                logger.info("密文解密后有" + count + "组密文可以解密出0，验证通过！");
            }else {
                logger.error("验证未通过！");
            }
        }
    }
}
