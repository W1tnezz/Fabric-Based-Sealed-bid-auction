package sdkInit

import (
	"fmt"
	"github.com/hyperledger/fabric-sdk-go/pkg/client/channel"
)

func (t *Application) Get(args []string) (string, error) {
	response, err := t.SdkEnvInfo.Client.Query(channel.Request{ChaincodeID: t.SdkEnvInfo.ChaincodeID, Fcn: args[0], Args:[][]byte{[]byte(args[1])}},channel.WithTargetEndpoints("peer1.org2.example.com"))
	if err != nil {
		return "", fmt.Errorf("failed to query: %v", err)
	}
	return string(response.Payload), nil
}