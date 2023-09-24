package com.njupt.chaincode;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import java.nio.charset.StandardCharsets;

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
    public String clear(final Context ctx){
        ChaincodeStub stub = ctx.getStub();
        StringBuilder sb = new StringBuilder();
        for (String arg : stub.getStringArgs()) {
            sb.append(arg);
        }
        return sb.toString();
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String submitFirstRoundInput(final Context ctx, final String identity, final String input){
        ChaincodeStub stub = ctx.getStub();
        stub.putStringState(identity + "_first_round_input", input);
        stub.setEvent("newFirstRoundInput", input.getBytes(StandardCharsets.ISO_8859_1));
        return stub.getTxId();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryFirstRoundInputById(final Context ctx, final String identity){
        ChaincodeStub stub = ctx.getStub();
        return stub.getStringState(identity + "_first_round_input");
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String submitSecondRoundInput(final Context ctx, final String identity, final String input){
        ChaincodeStub stub = ctx.getStub();
        stub.putStringState(identity + "_second_round_input", input);
        stub.setEvent("newSecondRoundInput", input.getBytes(StandardCharsets.ISO_8859_1));
        return stub.getTxId();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String querySecondRoundInputById(final Context ctx, final String identity){
        ChaincodeStub stub = ctx.getStub();
        return stub.getStringState(identity + "_second_round_input");
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String submitThirdRoundInput(final Context ctx, final String identity, final String input){
        ChaincodeStub stub = ctx.getStub();
        stub.putStringState(identity + "_third_round_input", input);
        stub.setEvent("newThirdRoundInput", input.getBytes(StandardCharsets.ISO_8859_1));
        return stub.getTxId();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryThirdRoundInputById(final Context ctx, final String identity){
        ChaincodeStub stub = ctx.getStub();
        return stub.getStringState(identity + "_third_round_input");
    }
}
