# Fabric-Test-Network-Startup

手动启动Fabric流程，参考自https://www.bilibili.com/video/BV1SR4y1X7uB/?spm_id_from=333.788&vd_source=045d76213134eb86118d2f7ffdde4a4b

### 配置文件：

##### 生成crypto-config.yaml文件

`cryptogen showtemplate > crypto-config.yaml`

##### 根据crypto-config.yaml生成证书等文件

`cryptogen generate --config=crypto-config.yaml`

##### 生成创世区块

`configtxgen -profile TwoOrgsOrdererGenesis -outputBlock ./channel-artifacts/genesis.block -channelID test-channel`

##### 生成通道文件

`configtxgen -profile TwoOrgsChannel -outputCreateChannelTx ./channel-artifacts/channel.tx -channelID mychannel`

##### 更新锚节点

`configtxgen -outputAnchorPeersUpdate ./channel-artifacts/Org1MSPanchors.tx -profile TwoOrgsChannel -channelID mychannel -asOrg Org1MSP`

`configtxgen -outputAnchorPeersUpdate ./channel-artifacts/Org2MSPanchors.tx -profile TwoOrgsChannel -channelID mychannel -asOrg Org2MSP`



### 启动：

##### 启动Docker环境：

`sudo docker-compose up -d `

关闭指令：

`sudo docker-compose down -v`

##### 进入cli1环境：

`sudo docker exec -it cli1 bash`

##### 创建通道：

`peer channel create -o orderer.example.com:7050 -c mychannel -f ./channel-artifacts/channel.tx --tls true --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/example.com/msp/tlscacerts/tlsca.example.com-cert.pem`

##### 加入通道：

`peer channel join -b mychannel.block`

##### 更新锚节点：

`peer channel update -o orderer.example.com:7050 -c mychannel -f ./channel-artifacts/Org1MSPanchors.tx --tls true --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem`

##### 将网络中的节点都加入到通道中：

从docker中拷贝出来：

`sudo docker cp cli1:/opt/gopath/src/github.com/hyperledger/fabric/peer/mychannel.block ./`

拷贝到其他节点中：

`sudo docker cp mychannel.block cli2:/opt/gopath/src/github.com/hyperledger/fabric/peer/
sudo docker cp mychannel.block cli3:/opt/gopath/src/github.com/hyperledger/fabric/peer/
sudo docker cp mychannel.block cli4:/opt/gopath/src/github.com/hyperledger/fabric/peer/`

进入其他节点终端并加入通道：

`sudo docker exec -it cli2 bash`

`peer channel join -b mychannel.block`

其中在cli3终端下加入通道后还需要更新锚节点Org2：

`peer channel update -o orderer.example.com:7050 -c mychannel -f ./channel-artifacts/Org2MSPanchors.tx --tls true --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem`

##### 在cli1下查看区块信息和通道信息：

`peer channel getinfo -c mychannel`





## 安装链码：

##### 将sacc文件夹复制到chaincode文件夹下

`sudo cp -r ./sacc /home/indistinguishable/fabric-test/chaincode/go/`

##### 客户端下安装链码

在sacc文件夹下进行：

`go env -w GOPROXY=https:goproxy.cn,direct`

`go mod vendor`

##### 部署链码

回到peer目录

`cd /opt/gopath/src/github.com/hyperledger/fabric/peer/`

打包

`peer lifecycle chaincode package sacc.tar.gz --path /opt/gopath/src/github.com/hyperledger/multiple-deployment/chaincode/go/sacc/ --label sacc_1`

安装

`peer lifecycle chaincode install sacc.tar.gz`

查询安装情况

`peer lifecycle chaincode queryinstalled`

##### 部署到其他节点

机械操作

sudo docker cp cli1:/opt/gopath/src/github.com/hyperledger/fabric/peer/sacc.tar.gz ./

sudo docker cp sacc.tar.gz cli2:/opt/gopath/src/github.com/hyperledger/fabric/peer/

##### 批准链码

**cli1和cli3下执行(注意package-id可以在部署链码时查到)：**

peer lifecycle chaincode approveformyorg --channelID mychannel --name sacc --version 1.0 --init-required --package-id sacc_1:b33357c4012471d8bd96ba48fd2a12ada5fedfbfd6d623590295778500a0368d --sequence 1 --tls true --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem

**查询批准情况：**

peer lifecycle chaincode checkcommitreadiness --channelID mychannel --name sacc --version 1.0 --init-required --sequence 1 --tls true --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem --output json

##### 提交链码：

cli1或cli3上执行：

peer lifecycle chaincode commit -o orderer.example.com:7050 --channelID mychannel --name sacc --version 1.0 --init-required --sequence 1 --tls true --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem --peerAddresses peer0.org1.example.com:7051 --tlsRootCertFiles /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt --peerAddresses peer0.org2.example.com:8051 --tlsRootCertFiles /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt

##### 初始化链码：

只需要调用一次，链码初始化之后，初始化参数会广播给其他节点

peer chaincode invoke -o orderer.example.com:7050 --isInit --ordererTLSHostnameOverride orderer.example.com --tls true --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem -C mychannel -n sacc --peerAddresses peer0.org1.example.com:7051 --tlsRootCertFiles /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt --peerAddresses peer0.org2.example.com:8051 --tlsRootCertFiles /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt -c '{"Args":["a","bb"]}'

##### 查询

两种方式：

本地查询：

peer chaincode query -C mychannel -n sacc -c '{"Args":["a","bb"]}'

上链查询：

peer chaincode invoke -o orderer.example.com:7050 --ordererTLSHostnameOverride orderer.example.com --tls true --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem -C mychannel -n mycc --peerAddresses peer0.org1.example.com:7051 --tlsRootCertFiles /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt --peerAddresses peer0.org2.example.com:8051 --tlsRootCertFiles /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt -c '{"Args":["queryAll"]}'







