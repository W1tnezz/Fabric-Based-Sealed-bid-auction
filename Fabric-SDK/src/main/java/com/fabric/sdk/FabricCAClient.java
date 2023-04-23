package com.fabric.sdk;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.Properties;

/**
 * @description CA注册用户的client
 */
public class FabricCAClient {
    private HFCAClient hfcaClient;
    public FabricCAClient(String url, Properties properties) throws MalformedURLException, IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException {
        hfcaClient = HFCAClient.createNewInstance(url,properties);
        CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
        hfcaClient.setCryptoSuite(cryptoSuite);
    }
    /**
     * @description 注册用户
     * @param registar 注册管理员
     * @param register 被注册人员
     * @return secret 密码随机生成的字符串
     * @throws Exception
     */
    public String register(UserContext registar,UserContext register) throws Exception {
        RegistrationRequest request = new RegistrationRequest(register.getName(),register.getAffiliation());
        String secret = hfcaClient.register(request,registar);
        return secret;
    }
    /**
     * @description 登录用户
     * @param username
     * @param password
     * @return enrollment 从CA服务器端拿到私钥和证书信息到java对象中
     * @throws EnrollmentException
     * @throws org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException
     */
    public Enrollment enroll(String username,String password) throws EnrollmentException, org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException {
        Enrollment enrollment =  hfcaClient.enroll(username,password);
        return enrollment;
    }
}
