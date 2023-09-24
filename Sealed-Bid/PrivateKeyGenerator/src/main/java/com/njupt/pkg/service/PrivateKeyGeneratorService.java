package com.njupt.pkg.service;

import it.unisa.dia.gas.jpbc.Element;

public interface PrivateKeyGeneratorService {
    public Element getMasterPublicKey();
    public Element getUserSecretKey(String userIdentity);
}
