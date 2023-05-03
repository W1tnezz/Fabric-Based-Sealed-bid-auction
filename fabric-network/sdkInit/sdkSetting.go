package sdkInit

import (
	"fmt"
	mb "github.com/hyperledger/fabric-protos-go/msp"
	pb "github.com/hyperledger/fabric-protos-go/peer"
	"github.com/hyperledger/fabric-sdk-go/pkg/client/channel"
	mspclient "github.com/hyperledger/fabric-sdk-go/pkg/client/msp"
	"github.com/hyperledger/fabric-sdk-go/pkg/client/resmgmt"
	"github.com/hyperledger/fabric-sdk-go/pkg/common/errors/retry"
	"github.com/hyperledger/fabric-sdk-go/pkg/common/errors/status"
	contextAPI "github.com/hyperledger/fabric-sdk-go/pkg/common/providers/context"
	fabAPI "github.com/hyperledger/fabric-sdk-go/pkg/common/providers/fab"
	"github.com/hyperledger/fabric-sdk-go/pkg/common/providers/msp"
	contextImpl "github.com/hyperledger/fabric-sdk-go/pkg/context"
	"github.com/hyperledger/fabric-sdk-go/pkg/core/config"
	lcpackager "github.com/hyperledger/fabric-sdk-go/pkg/fab/ccpackager/lifecycle"
	"github.com/hyperledger/fabric-sdk-go/pkg/fabsdk"
	"github.com/hyperledger/fabric-sdk-go/third_party/github.com/hyperledger/fabric/common/policydsl"
	"time"
)

func Setup(configFile string, info *SdkEnvInfo) (*fabsdk.FabricSDK, error) {
	//初始化sdk
	var err error
	sdk, err := fabsdk.New(config.FromFile(configFile))
	if err != nil {
		return nil, err
	}
	// 为组织获得Client句柄和Context信息
	for _, org := range info.Orgs {
		org.orgMspClient, err = mspclient.New(sdk.Context(), mspclient.WithOrg(org.OrgName))
		if err != nil {
			return nil, err
		}
		orgContext := sdk.Context(fabsdk.WithUser(org.OrgAdminUser), fabsdk.WithOrg(org.OrgName))
		org.OrgAdminClientContext = &orgContext
		resMgmtClient, err := resmgmt.New(orgContext)
		if err != nil {
			return nil, fmt.Errorf("根据指定的资源管理客户端Context创建通道管理客户端失败: %v", err)
		}
		org.OrgResMgmt = resMgmtClient
		//设置peer
		orgPeers, err := DiscoverLocalPeers(*org.OrgAdminClientContext, org.OrgPeerNum)
		if err != nil {
			return sdk, fmt.Errorf("DiscoverLocalPeers error: %v", err)
		}
		org.Peers = orgPeers
	}
	// 为Orderer获得Context信息
	ordererClientContext := sdk.Context(fabsdk.WithUser(info.OrdererAdminUser), fabsdk.WithOrg(info.OrdererOrgName))
	info.OrdererClientContext = &ordererClientContext
	return sdk, nil
}

//遍历peer
func DiscoverLocalPeers(ctxProvider contextAPI.ClientProvider, expectedPeers int) ([]fabAPI.Peer, error) {
	ctx, err := contextImpl.NewLocal(ctxProvider)
	if err != nil {
		return nil, fmt.Errorf("error creating local context: %v", err)
	}
	discoveredPeers, err := retry.NewInvoker(retry.New(retry.TestRetryOpts)).Invoke(
		func() (interface{}, error) {
			peers, serviceErr := ctx.LocalDiscoveryService().GetPeers()
			if serviceErr != nil {
				return nil, fmt.Errorf("getting peers for MSP [%s] error: %v", ctx.Identifier().MSPID, serviceErr)
			}
			if len(peers) < expectedPeers {
				return nil, status.New(status.TestStatus, status.GenericTransient.ToInt32(), fmt.Sprintf("Expecting %d peers but got %d", expectedPeers, len(peers)), nil)
			}
			return peers, nil
		},
	)
	if err != nil {
		return nil, err
	}
	return discoveredPeers.([]fabAPI.Peer), nil
}

//create channel
func CreateChannel(info *SdkEnvInfo) error {
	fmt.Println(">> 开始创建通道......")
	if len(info.Orgs) == 0 {
		return fmt.Errorf("通道组织不能为空，请提供组织信息")
	}
	// 获得所有组织的签名信息
	signIds := []msp.SigningIdentity{}
	for _, org := range info.Orgs {
		// 获取创建通道请求使用的签名
		orgSignId, err := org.orgMspClient.GetSigningIdentity(org.OrgAdminUser)
		if err != nil {
			return fmt.Errorf("GetSigningIdentity error: %v", err)
		}
		signIds = append(signIds, orgSignId)
	}
	chMgmtClient, err := resmgmt.New(*info.OrdererClientContext)
	if err != nil {
		return fmt.Errorf("Channel management client create error: %v", err)
	}
	// 创建通道
	req := resmgmt.SaveChannelRequest{ChannelID: info.ChannelID,
		ChannelConfigPath: info.ChannelConfig,
		SigningIdentities: signIds}
	if _, err := chMgmtClient.SaveChannel(req, resmgmt.WithRetry(retry.DefaultResMgmtOpts), resmgmt.WithOrdererEndpoint(info.OrdererEndpoint)); err != nil {
		return fmt.Errorf("error should be nil for SaveChannel of orgchannel: %v", err)
	}
	fmt.Println(">> 使用每个org的管理员身份更新锚节点配置...")
	//更新锚节点
	for i, org := range info.Orgs {
		req = resmgmt.SaveChannelRequest{ChannelID: info.ChannelID,
			ChannelConfigPath: org.OrgAnchorFile,
			SigningIdentities: []msp.SigningIdentity{signIds[i]}}
		if _, err = org.OrgResMgmt.SaveChannel(req, resmgmt.WithRetry(retry.DefaultResMgmtOpts), resmgmt.WithOrdererEndpoint(info.OrdererEndpoint)); err != nil {
			return fmt.Errorf("SaveChannel for anchor org %s error: %v", org.OrgName, err)
		}
	}
	fmt.Println(">> 使用每个org的管理员身份更新锚节点配置完成")
	fmt.Println(">> 创建通道成功")
	return nil
}

func JoinChannel(info *SdkEnvInfo) error {
	fmt.Println(">> 加入通道......")
	for _, org := range info.Orgs {
		// 加入通道
		if err := org.OrgResMgmt.JoinChannel(info.ChannelID, resmgmt.WithRetry(retry.DefaultResMgmtOpts), resmgmt.WithOrdererEndpoint(info.OrdererEndpoint)); err != nil {
			return fmt.Errorf("%s peers failed to JoinChannel: %v", org.OrgName, err)
		}
	}
	fmt.Println(">> 加入通道成功")
	return nil
}

//chaincode
func InstallCC(info *SdkEnvInfo) (string, error) {
	if len(info.Orgs) == 0 {
		return "", fmt.Errorf("the number of organization should not be zero.")
	}
	// 打包链码
	fmt.Println(">> 开始打包链码......")
	ccName := info.ChaincodeID
	ccVersion := info.ChaincodeVersion
	ccpath := info.ChaincodePath
	label := ccName + "_" + ccVersion
	desc := &lcpackager.Descriptor{
		Path:  ccpath,
		Type:  pb.ChaincodeSpec_JAVA,
		Label: label,
	}
	ccPkg, err := lcpackager.NewCCPackage(desc)
	if err != nil {
		return "", fmt.Errorf("Package chaincode source error: %v", err)
	}
	label = desc.Label
	//打包
	packageID := lcpackager.ComputePackageID(label, ccPkg)
	fmt.Println(">> 打包链码成功, PackageID: ", packageID)

	// 安装链码
	fmt.Println(">> 开始安装链码......")
	installCCReq := resmgmt.LifecycleInstallCCRequest{
		Label:   label,
		Package: ccPkg,
	}
	for _, org := range info.Orgs {
		orgPeers, err := DiscoverLocalPeers(*org.OrgAdminClientContext, org.OrgPeerNum)
		if err != nil {
			fmt.Errorf("DiscoverLocalPeers error: %v", err)
		}
		//是否存在该版本的链码，不存在就安装
		if flag, _ := checkInstalled(packageID, orgPeers[0], org.OrgResMgmt); flag == false {
			if _, err := org.OrgResMgmt.LifecycleInstallCC(installCCReq, resmgmt.WithTargets(orgPeers...), resmgmt.WithRetry(retry.DefaultResMgmtOpts)); err != nil {
				return "", fmt.Errorf("LifecycleInstallCC error: %v", err)
			}
		}
		//for _, peer := range orgPeers {
		//	if flag, _ := checkInstalled(packageID, peer, org.OrgResMgmt); flag == false {
		//		return fmt.Errorf("LifecycleInstallCC error: %v", err)
		//	}
		//}
	}
	// 获取已经安装在节点的package
	if err := getInstalledCCPackage(packageID, info.Orgs[0]); err != nil {
		return "", fmt.Errorf("getInstalledCCPackage error: %v", err)
	}
	fmt.Println(">> 安装链码成功")
	return packageID, nil
}
func checkInstalled(packageID string, peer fabAPI.Peer, client *resmgmt.Client) (bool, error) {
	flag := false
	resp1, err := client.LifecycleQueryInstalledCC(resmgmt.WithTargets(peer))
	if err != nil {
		return flag, fmt.Errorf("LifecycleQueryInstalledCC error: %v", err)
	}
	for _, t := range resp1 {
		if t.PackageID == packageID {
			flag = true
		}
	}
	return flag, nil
}
func getInstalledCCPackage(packageID string, org *OrgInfo) error {
	// use org1
	orgPeers, err := DiscoverLocalPeers(*org.OrgAdminClientContext, 1)
	if err != nil {
		return fmt.Errorf("DiscoverLocalPeers error: %v", err)
	}

	if _, err := org.OrgResMgmt.LifecycleGetInstalledCCPackage(packageID, resmgmt.WithTargets([]fabAPI.Peer{orgPeers[0]}...)); err != nil {
		return fmt.Errorf("LifecycleGetInstalledCCPackage error: %v", err)
	}
	return nil
}

func ApproveLifecycle(info *SdkEnvInfo, sequence int64, packageID string) error {
	if len(info.Orgs) == 0 {
		return fmt.Errorf("the number of organization should not be zero.")
	}
	// 组织批准合约
	fmt.Println(">> 组织认可智能合约定义......")
	if err := approveCC(packageID, info.ChaincodeID, info.ChaincodeVersion, sequence, info.ChannelID, info.Orgs, info.OrdererEndpoint); err != nil {
		return fmt.Errorf("approveCC error: %v", err)
	}
	// 查询状态
	if err := queryApprovedCC(info.ChaincodeID, sequence, info.ChannelID, info.Orgs); err != nil {
		return fmt.Errorf("queryApprovedCC error: %v", err)
	}
	fmt.Println(">> 组织认可智能合约定义完成")

	// 检查智能合约
	fmt.Println(">> 检查智能合约是否就绪......")
	if err := checkCCCommitReadiness(packageID, info.ChaincodeID, info.ChaincodeVersion, sequence, info.ChannelID, info.Orgs); err != nil {
		return fmt.Errorf("checkCCCommitReadiness error: %v", err)
	}
	fmt.Println(">> 智能合约已经就绪")

	time.Sleep(4 * time.Second)

	// 提交合约
	fmt.Println(">> 提交智能合约定义......")
	if err := commitCC(info.ChaincodeID, info.ChaincodeVersion, sequence, info.ChannelID, info.Orgs, info.OrdererEndpoint); err != nil {
		return fmt.Errorf("commitCC error: %v", err)
	}
	// 查询状态
	if err := queryCommittedCC(info.ChaincodeID, info.ChannelID, sequence, info.Orgs); err != nil {
		return fmt.Errorf("queryCommittedCC error: %v", err)
	}
	fmt.Println(">> 智能合约定义提交完成")
	return nil
}
func approveCC(packageID string, ccName, ccVersion string, sequence int64, channelID string, orgs []*OrgInfo, ordererEndpoint string) error {
	mspIDs := []string{}
	for _, org := range orgs {
		mspIDs = append(mspIDs, org.OrgMspId)
	}
	ccPolicy := policydsl.SignedByNOutOfGivenRole(int32(len(mspIDs)), mb.MSPRole_MEMBER, mspIDs)
	approveCCReq := resmgmt.LifecycleApproveCCRequest{
		Name:              ccName,
		Version:           ccVersion,
		PackageID:         packageID,
		Sequence:          sequence,
		EndorsementPlugin: "escc",
		ValidationPlugin:  "vscc",
		SignaturePolicy:   ccPolicy,
		InitRequired:      true,
	}

	for _, org := range orgs {
		orgPeers, err := DiscoverLocalPeers(*org.OrgAdminClientContext, org.OrgPeerNum)
		fmt.Printf(">>> chaincode approved by %s peers:\n", org.OrgName)
		for _, p := range orgPeers {
			fmt.Printf("	%s\n", p.URL())
		}

		if err != nil {
			return fmt.Errorf("DiscoverLocalPeers error: %v", err)
		}
		if _, err := org.OrgResMgmt.LifecycleApproveCC(channelID, approveCCReq, resmgmt.WithTargets(orgPeers...), resmgmt.WithOrdererEndpoint(ordererEndpoint), resmgmt.WithRetry(retry.DefaultResMgmtOpts)); err != nil {
			return fmt.Errorf("LifecycleApproveCC error: %v", err)
		}
	}
	return nil
}

func queryApprovedCC(ccName string, sequence int64, channelID string, orgs []*OrgInfo) error {
	queryApprovedCCReq := resmgmt.LifecycleQueryApprovedCCRequest{
		Name:     ccName,
		Sequence: sequence,
	}

	for _, org := range orgs {
		orgPeers, err := DiscoverLocalPeers(*org.OrgAdminClientContext, org.OrgPeerNum)
		if err != nil {
			return fmt.Errorf("DiscoverLocalPeers error: %v", err)
		}
		// Query approve cc
		for _, p := range orgPeers {
			resp, err := retry.NewInvoker(retry.New(retry.TestRetryOpts)).Invoke(
				func() (interface{}, error) {
					resp1, err := org.OrgResMgmt.LifecycleQueryApprovedCC(channelID, queryApprovedCCReq, resmgmt.WithTargets(p))
					if err != nil {
						return nil, status.New(status.TestStatus, status.GenericTransient.ToInt32(), fmt.Sprintf("LifecycleQueryApprovedCC returned error: %v", err), nil)
					}
					return resp1, err
				},
			)
			if err != nil {
				return fmt.Errorf("Org %s Peer %s NewInvoker error: %v", org.OrgName, p.URL(), err)
			}
			if resp == nil {
				return fmt.Errorf("Org %s Peer %s Got nil invoker", org.OrgName, p.URL())
			}
		}
	}
	return nil
}

func checkCCCommitReadiness(packageID string, ccName, ccVersion string, sequence int64, channelID string, orgs []*OrgInfo) error {
	mspIds := []string{}
	for _, org := range orgs {
		mspIds = append(mspIds, org.OrgMspId)
	}
	ccPolicy := policydsl.SignedByNOutOfGivenRole(int32(len(mspIds)), mb.MSPRole_MEMBER, mspIds)
	req := resmgmt.LifecycleCheckCCCommitReadinessRequest{
		Name:    ccName,
		Version: ccVersion,
		//PackageID:         packageID,
		EndorsementPlugin: "escc",
		ValidationPlugin:  "vscc",
		SignaturePolicy:   ccPolicy,
		Sequence:          sequence,
		InitRequired:      true,
	}
	for _, org := range orgs {
		orgPeers, err := DiscoverLocalPeers(*org.OrgAdminClientContext, org.OrgPeerNum)
		if err != nil {
			fmt.Errorf("DiscoverLocalPeers error: %v", err)
		}
		for _, p := range orgPeers {
			resp, err := retry.NewInvoker(retry.New(retry.TestRetryOpts)).Invoke(
				func() (interface{}, error) {
					resp1, err := org.OrgResMgmt.LifecycleCheckCCCommitReadiness(channelID, req, resmgmt.WithTargets(p))
					fmt.Printf("LifecycleCheckCCCommitReadiness cc = %v, = %v\n", ccName, resp1)
					if err != nil {
						return nil, status.New(status.TestStatus, status.GenericTransient.ToInt32(), fmt.Sprintf("LifecycleCheckCCCommitReadiness returned error: %v", err), nil)
					}
					flag := true
					for _, r := range resp1.Approvals {
						flag = flag && r
					}
					if !flag {
						return nil, status.New(status.TestStatus, status.GenericTransient.ToInt32(), fmt.Sprintf("LifecycleCheckCCCommitReadiness returned : %v", resp1), nil)
					}
					return resp1, err
				},
			)
			if err != nil {
				return fmt.Errorf("NewInvoker error: %v", err)
			}
			if resp == nil {
				return fmt.Errorf("got nil invoker response")
			}
		}
	}

	return nil
}

func commitCC(ccName, ccVersion string, sequence int64, channelID string, orgs []*OrgInfo, ordererEndpoint string) error {
	mspIDs := []string{}
	for _, org := range orgs {
		mspIDs = append(mspIDs, org.OrgMspId)
	}
	ccPolicy := policydsl.SignedByNOutOfGivenRole(int32(len(mspIDs)), mb.MSPRole_MEMBER, mspIDs)

	req := resmgmt.LifecycleCommitCCRequest{
		Name:              ccName,
		Version:           ccVersion,
		Sequence:          sequence,
		EndorsementPlugin: "escc",
		ValidationPlugin:  "vscc",
		SignaturePolicy:   ccPolicy,
		InitRequired:      true,
	}
	_, err := orgs[0].OrgResMgmt.LifecycleCommitCC(channelID, req, resmgmt.WithOrdererEndpoint(ordererEndpoint), resmgmt.WithRetry(retry.DefaultResMgmtOpts))
	if err != nil {
		return fmt.Errorf("LifecycleCommitCC error: %v", err)
	}
	return nil
}

func queryCommittedCC(ccName string, channelID string, sequence int64, orgs []*OrgInfo) error {
	req := resmgmt.LifecycleQueryCommittedCCRequest{
		Name: ccName,
	}

	for _, org := range orgs {
		orgPeers, err := DiscoverLocalPeers(*org.OrgAdminClientContext, org.OrgPeerNum)
		if err != nil {
			return fmt.Errorf("DiscoverLocalPeers error: %v", err)
		}
		for _, p := range orgPeers {
			resp, err := retry.NewInvoker(retry.New(retry.TestRetryOpts)).Invoke(
				func() (interface{}, error) {
					resp1, err := org.OrgResMgmt.LifecycleQueryCommittedCC(channelID, req, resmgmt.WithTargets(p))
					if err != nil {
						return nil, status.New(status.TestStatus, status.GenericTransient.ToInt32(), fmt.Sprintf("LifecycleQueryCommittedCC returned error: %v", err), nil)
					}
					flag := false
					for _, r := range resp1 {
						if r.Name == ccName && r.Sequence == sequence {
							flag = true
							break
						}
					}
					if !flag {
						return nil, status.New(status.TestStatus, status.GenericTransient.ToInt32(), fmt.Sprintf("LifecycleQueryCommittedCC returned : %v", resp1), nil)
					}
					return resp1, err
				},
			)
			if err != nil {
				return fmt.Errorf("NewInvoker error: %v", err)
			}
			if resp == nil {
				return fmt.Errorf("Got nil invoker response")
			}
		}
	}
	return nil
}
func InitCC(info *SdkEnvInfo, upgrade bool, sdk *fabsdk.FabricSDK) error {
	ccName := info.ChaincodeID
	channelID := info.ChannelID
	org := info.Orgs[0]
	clientChannelContext := sdk.ChannelContext(channelID, fabsdk.WithUser(org.OrgUser), fabsdk.WithOrg(org.OrgName))
	client, err := channel.New(clientChannelContext)
	if err != nil {
		return fmt.Errorf("Failed to create new channel client: %s", err)
	}
	_, err = client.Execute(channel.Request{ChaincodeID: ccName, Fcn: "init", Args: [][]byte{}, IsInit: true},
		channel.WithRetry(retry.DefaultChannelOpts))
	if err != nil {
		return fmt.Errorf("Failed to init: %s", err)
	}
	return nil
}
func (t *SdkEnvInfo) InitService(chaincodeID, channelID string, org *OrgInfo, sdk *fabsdk.FabricSDK) error {
	handler := &SdkEnvInfo{
		ChaincodeID: chaincodeID,
	}
	clientChannelContext := sdk.ChannelContext(channelID, fabsdk.WithUser(org.OrgUser), fabsdk.WithOrg(org.OrgName))
	var err error
	t.Client, err = channel.New(clientChannelContext)
	if err != nil {
		return nil
	}
	handler.Client = t.Client
	return nil
}
