package com.njupt.pkg.service.impl;

import com.njupt.pkg.dao.PrivateKeyGenerator;
import com.njupt.pkg.service.PrivateKeyGeneratorService;
import it.unisa.dia.gas.jpbc.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PrivateKeyGeneratorServiceImpl implements PrivateKeyGeneratorService {
    @Autowired
    private PrivateKeyGenerator pkg;
    @Override
    public Element getMasterPublicKey() {
        return pkg.getMasterPublicKey();
    }
    @Override
    public Element getUserSecretKey(String userIdentity) {
        return pkg.getUserPrivateKey(userIdentity);
    }
}
