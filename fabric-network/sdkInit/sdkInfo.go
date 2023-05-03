package sdkInit

import (
	"github.com/hyperledger/fabric-sdk-go/pkg/client/channel"
	mspclient "github.com/hyperledger/fabric-sdk-go/pkg/client/msp"
	"github.com/hyperledger/fabric-sdk-go/pkg/client/resmgmt"
	contextAPI "github.com/hyperledger/fabric-sdk-go/pkg/common/providers/context"
	"github.com/hyperledger/fabric-sdk-go/pkg/common/providers/fab"
)

// org info
type OrgInfo struct {
	OrgAdminUser          string            // admin
	OrgName               string            // name
	OrgMspId              string            // msp
	OrgUser               string            // user
	orgMspClient          *mspclient.Client //running
	OrgAdminClientContext *contextAPI.ClientProvider
	OrgResMgmt            *resmgmt.Client
	OrgPeerNum            int        //num
	Peers                 []fab.Peer //peers
	OrgAnchorFile         string
}

// all info
type SdkEnvInfo struct {
	// channel info
	ChannelID     string
	ChannelConfig string

	// orgs
	Orgs []*OrgInfo
	// orderder info
	OrdererAdminUser     string
	OrdererOrgName       string
	OrdererEndpoint      string
	OrdererClientContext *contextAPI.ClientProvider
	// chaincode info
	ChaincodeID      string
	ChaincodeGoPath  string
	ChaincodePath    string
	ChaincodeVersion string
	Client           *channel.Client
}

type Application struct {
	SdkEnvInfo *SdkEnvInfo
}
