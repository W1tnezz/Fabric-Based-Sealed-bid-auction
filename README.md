# Fabric-Based-Sealed-bid-auction
## 1. 简介

基于Hyperledger Fabric区块链和身份基同态加密算法的密封电子拍卖协议。

该项目是本人论文的实现工作，如果需要了解协议的理论部分可以参考：http://jcs.iie.ac.cn/xxaqxb/ch/reader/view_abstract.aspx?flag=2&file_no=202305220000003&journal_id=xxaqxb。



## 2. 运行环境

| 依赖               | 版本    | 说明                                                         |
| ------------------ | ------- | ------------------------------------------------------------ |
| Golang             | 1.15.11 | 建议不要使用太高版本，高版本使用Fabric-SDK存在Bug            |
| Open-JDK           | 17      | 不确定其它版本的JDK和目前maven引入的依赖版本是否兼容         |
| Hyperledger Fabric | 2.4.6   | 安装Docker和Docker-Compose后，启动之后会自动拉取该版本的镜像 |
| Docker             | 24.0.2  |                                                              |
| Docker-Compose     | 1.25.0  |                                                              |



## 3. 项目结构

```
Fabric-Based-Sealed-bid-auction/
├── fabric-network                      // Fabric网络配置文件以及启动代码(参考自B站：BV1SR4y1X7uB)
│   ├── chaincode                          // 链码文件夹
│   ├── config.yaml                        // 网络节点配置文件
│   ├── fixtures                           // 固定配置文件，包括节点的证书、Docker-Compose的启动配置文件
│   ├── go.mod                             // Go依赖
│   ├── go.sum
│   ├── main.go                            // 基于Fabric-Go-SDK执行创建通道、加入通道、部署链码等一系列流程的源码
│   ├── Network-Startup.md                 // 手动完成通道创建等一系列流程的终端命令
│   └── sdkInit                            // Go Lib
├── README.md
└── Sealed-Bid                          // 拍卖程序，基于Java
    ├── Benchmark                          // 论文实验
    ├── Bidder                             // 拍卖参与者程序，实现了论文协议的三轮交互+结果验证
    ├── Fabric-Chaincode                   // 链码源码，maven打包为jar包后放到/fabric-network/chaincode/java下
    ├── pom.xml
    └── PrivateKeyGenerator                // 私钥生成中心PKG，身份基密码中的角色，使用SpringBootWeb实现
```



## 4. 启动

#### 1. 启动Fabric网络

项目采用的网络结构为两个组织，每个组织下两个peer节点，共计4个peer节点，**进入/fabric-network/fixtures/目录**下执行命令：

```shell
$ docker-compose up -d
```

正常启动的终端输出如下：

```
Creating network "fixtures_testwork" with the default driver
Creating volume "fixtures_orderer.example.com" with default driver
Creating volume "fixtures_peer0.org1.example.com" with default driver
Creating volume "fixtures_peer1.org1.example.com" with default driver
Creating volume "fixtures_peer0.org2.example.com" with default driver
Creating volume "fixtures_peer1.org2.example.com" with default driver
Creating peer1.org1.example.com ... done
Creating peer1.org2.example.com ... done
Creating peer0.org1.example.com ... done
Creating peer0.org2.example.com ... done
Creating orderer.example.com    ... done
Creating cli1                   ... done
Creating cli2                   ... done
Creating cli4                   ... done
Creating cli3                   ... done
```

通过docker命令查看容器运行状态：

```shell
$ docker ps
```

正常运行启动的话可以看到4个tools容器、4个peers节点容器、1个orderer节点容器，注意容器的Status是否正常。

#### 2.  创建通道、部署链码

编译fabric-network下的Go源码，进入/fabric-network/目录，执行如下命令：

```shell
$ go build -o ./fabric-starter
```

编译得到可执行文件，**将文件放在/fabric-network/目录下运行**：

```shell
$ ./fabric-starter
```

#### 3. 启动私钥生成中心

运行PrivateKeyGenerator下的main方法

#### 4. 启动4个Bidder程序

推荐在Idea中配置4个Configuration，运行参数如下：

```
--n=4 --identity=bidder1 --filePath=./Bidder/src/main/resources/user-config/config-org1-peer0.yml
```

- n：参与拍卖的人数；
- identity：该bidder的身份公钥；
- filePath：该用户的配置信息文件路径；

启动完成后，bidder程序会自动进行拍卖流程。

#### 5. 关闭容器

**进入/fabric-network/fixtures/目录**下执行命令：

```shell
$ docker-compose down -v
```

