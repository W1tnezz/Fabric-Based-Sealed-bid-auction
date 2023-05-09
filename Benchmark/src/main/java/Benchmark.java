import crypto.CryptoUtils;
import it.unisa.dia.gas.jpbc.Element;

import java.util.ArrayList;
import java.util.List;

public class Benchmark {
    public static void main(String[] args) {
        int n = 100;
        System.out.println("-----" + n + "个参与方时的性能测试------");
        Element sk = CryptoUtils.getZr().newRandomElement().getImmutable();
        Element pk = CryptoUtils.getGroupGenerator().mulZn(sk);
        System.out.println("-------计算第一轮-------");
        List<Element[]> ciphers = new ArrayList<>();
        long time1 = System.currentTimeMillis();
        for (int i = 0; i < 32; i++) {
            Element[] cipher = CryptoUtils.encrypt(0, pk);
            ciphers.add(cipher);
        }
        long time2 = System.currentTimeMillis();
        System.out.println(">>> 第一轮计算耗时：" + (time2 - time1) + "ms");

        System.out.println("-------计算第二轮-------");
        List<List<Element[]>> ciphersS = new ArrayList<>();
        long time3 = System.currentTimeMillis();
        for (int i = 0; i < n - 1; i++) {
            ciphersS.add(CryptoUtils.DGK(ciphers));
        }
        long time4 = System.currentTimeMillis();
        System.out.println(">>> 第二轮计算耗时：" + (time4 - time3)/(n-1) + "ms/方");

        System.out.println("-------计算第三轮-------");
        long time5 = System.currentTimeMillis();
        CryptoUtils.calTokens(ciphersS, sk);
        long time6 = System.currentTimeMillis();
        System.out.println(">>> 第三轮计算耗时：" + (time6 - time5)/(n-1) + "ms/方");
    }
}
