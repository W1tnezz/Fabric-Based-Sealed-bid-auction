package com.fabric.sdk;

public class Config {
    public static final String keyFolderPath = "./src/main/resources/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore";
    public static final String keyFileName="priv_sk";
    public static final String certFoldePath="./src/main/resources/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/signcerts";
    public static final String certFileName="Admin@org1.example.com-cert.pem";
    public static  final String tlsOrderFilePath = "./src/main/resources/crypto-config/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem";
    public static final String txfilePath = "E:/fabric/src/main/resources/test.tx";
    public static  final String tlsPeerFilePath = "./src/main/resources/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/msp/tlscacerts/tlsca.org1.example.com-cert.pem";
    public static  final String tlsPeerFilePathAddtion = "E:/fabric/src/main/resources/crypto-config/peerOrganizations/org2.example.com/tlsca/tlsca.org2.example.com-cert.pem";
}
