package com.fabric.sdk;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import java.io.Serializable;
import java.security.Security;
import java.util.Set;

/**
 * @description 用户对象
 */
public class UserContext implements User, Serializable {

    private String name;
    private Set<String> roles;
    private String account;
    private String affiliation;
    private Enrollment enrollment;
    private String mspId;
    static{
        Security.addProvider(new BouncyCastleProvider());
    }
    @Override
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    @Override
    public Set<String> getRoles() {
        return roles;
    }
    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
    @Override
    public String getAccount() {
        return account;
    }
    public void setAccount(String account) {
        this.account = account;
    }
    @Override
    public String getAffiliation() {
        return affiliation;
    }
    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }
    @Override
    public Enrollment getEnrollment() {
        return enrollment;
    }
    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }
    @Override
    public String getMspId() {
        return mspId;
    }
    public void setMspId(String mspId) {
        this.mspId = mspId;
    }
}

