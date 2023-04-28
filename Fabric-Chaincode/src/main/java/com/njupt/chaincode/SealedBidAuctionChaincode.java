package com.njupt.chaincode;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;

@Contract(name = "sealed-bid")
@Default
public class SealedBidAuctionChaincode implements ContractInterface {
    public SealedBidAuctionChaincode() {
    }

    /**
     * 初始化合约
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void init(final Context ctx){
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void clear(final Context ctx){
        ChaincodeStub stub = ctx.getStub();
        for (String str : stub.getStringArgs()) {
            stub.delState(str);
        }
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String submitFirstRoundInput(final Context ctx, final String identity, final byte[] input){
        ChaincodeStub stub = ctx.getStub();
        stub.putState(identity + " FirstRound", input);
        stub.setEvent("newFirstRoundInput", input);
        return stub.getTxId();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public byte[] queryFirstRoundInputById(final Context ctx, final String identity){
        ChaincodeStub stub = ctx.getStub();
        return stub.getState(identity + " FirstRound");
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String submitSecondRoundInput(final Context ctx, final String identity, final byte[] input){
        ChaincodeStub stub = ctx.getStub();
        stub.putState(identity + " SecondRound", input);
        stub.setEvent("newSecondRoundInput", input);
        return stub.getTxId();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public byte[] querySecondRoundInputById(final Context ctx, final String identity){
        ChaincodeStub stub = ctx.getStub();
        return stub.getState(identity + " SecondRound");
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String submitThirdRoundInput(final Context ctx, final String identity, final byte[] input){
        ChaincodeStub stub = ctx.getStub();
        stub.putState(identity + " ThirdRound", input);
        stub.setEvent("newThirdRoundInput", input);
        return stub.getTxId();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public byte[] queryThirdRoundInputById(final Context ctx, final String identity){
        ChaincodeStub stub = ctx.getStub();
        return stub.getState(identity + " ThirdRound");
    }
}
