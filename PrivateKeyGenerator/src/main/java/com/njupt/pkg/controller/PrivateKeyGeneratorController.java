package com.njupt.pkg.controller;

import com.njupt.pkg.service.PrivateKeyGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrivateKeyGeneratorController {
    @Autowired
    private PrivateKeyGeneratorService pkgService;
    @GetMapping("/getMPK")
    public byte[] handleGetMPK(){
        return pkgService.getMasterPublicKey().toBytes();
    }
    @GetMapping("/getSK/{identity}")
    public byte[] handleGetUserSK(@PathVariable("identity")String identity){
        return pkgService.getUserSecretKey(identity).toBytes();
    }
}
