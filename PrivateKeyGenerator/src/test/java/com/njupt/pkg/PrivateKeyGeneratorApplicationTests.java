package com.njupt.pkg;

import com.njupt.pkg.dao.PrivateKeyGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PrivateKeyGeneratorApplicationTests {

    @Autowired
    private PrivateKeyGenerator pkg;

    @Test
    void testPrivateKeyGenerator(){
        System.out.println(pkg.getMasterPublicKey());
        System.out.println(pkg.getUserPrivateKey("test"));
        System.out.println(pkg.getUserPrivateKey("test"));
        System.out.println(pkg.getUserPrivateKey("tttt"));
    }
}
