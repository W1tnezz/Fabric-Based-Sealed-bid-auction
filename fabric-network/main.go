package main

import (
	sdkInit "fabric-network/sdkInit"
	"fmt"
	"os"
)

var App sdkInit.Application

const CC_NAME string = "mycc"
const CC_VERSION string = "1.0.0"

func main() {
	fmt.Println("go")
	orgs := []*sdkInit.OrgInfo{
		{
			OrgAdminUser:  "Admin",
			OrgName:       "Org1",
			OrgMspId:      "Org1MSP",
			OrgUser:       "User1",
			OrgPeerNum:    2,
			OrgAnchorFile: "./fixtures/channel-artifacts/Org1MSPanchors.tx",
		},
		{
			OrgAdminUser:  "Admin",
			OrgName:       "Org2",
			OrgMspId:      "Org2MSP",
			OrgUser:       "User1",
			OrgPeerNum:    2,
			OrgAnchorFile: "./fixtures/channel-artifacts/Org2MSPanchors.tx",
		},
	}

	info := sdkInit.SdkEnvInfo{
		ChannelID:        "mychannel",
		ChannelConfig:    "./fixtures/channel-artifacts/channel.tx",
		Orgs:             orgs,
		OrdererAdminUser: "Admin",
		OrdererOrgName:   "OrdererOrg",
		OrdererEndpoint:  "orderer.example.com",
		ChaincodeID:      CC_NAME,
		ChaincodePath:    "./chaincode/java",
		ChaincodeVersion: CC_VERSION,
	}
	sdk, err := sdkInit.Setup("config.yaml", &info)
	if err != nil {
		fmt.Println(">> SDK set error: ", err)
		os.Exit(-1)
	}

	if err := sdkInit.CreateChannel(&info); err != nil {
		fmt.Println(">> Create channel error:", err)
		os.Exit(-1)
	}

	if err := sdkInit.JoinChannel(&info); err != nil {
		fmt.Println(">> Join channel error:", err)
		os.Exit(-1)
	}

	packageID, err := sdkInit.InstallCC(&info)
	if err != nil {
		fmt.Println(">> Install chaincode error: ", err)
		os.Exit(-1)
	}

	if err := sdkInit.ApproveLifecycle(&info, 1, packageID); err != nil {
		fmt.Println(">> approve chaincode error: ", err)
		os.Exit(-1)
	}

	if err := sdkInit.InitCC(&info, false, sdk); err != nil {
		fmt.Println(">> init chaincode error: ", err)
		os.Exit(-1)
	}

	if err := info.InitService(info.ChaincodeID, info.ChannelID, info.Orgs[0], sdk); err != nil {
		fmt.Println(">> init service error: ", err)
		os.Exit(-1)
	}

	//App = sdkInit.Application{
	//	SdkEnvInfo: &info,
	//}
	//
	//a := []string{"set", "a", "bcc"}
	//ret, err := App.Set(a)
	//if err != nil {
	//	fmt.Println(">> get error: ", err)
	//}
	//fmt.Println(">> result: ", ret)
	//
	//getArg := []string{"get", "a"}
	//ret, err = App.Get(getArg)
	//if err != nil {
	//	fmt.Println(">> get error: ", err)
	//}
	//fmt.Println(">> result: ", ret)
}
