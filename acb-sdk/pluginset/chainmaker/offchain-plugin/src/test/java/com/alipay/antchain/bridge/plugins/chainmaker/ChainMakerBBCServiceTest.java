/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.plugins.chainmaker;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.commons.core.sdp.ISDPMessage;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageFactory;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.Getter;
import lombok.Setter;
import org.chainmaker.pb.common.ChainmakerTransaction;
import org.chainmaker.pb.common.ContractOuterClass;
import org.chainmaker.pb.common.ResultOuterClass;
import org.chainmaker.sdk.utils.Utils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class ChainMakerBBCServiceTest {

    private static final String SDK_CONFIG_JSON = "chainmaker.json";
    private static final long rpcCallTimeout = 10000;
    private static final long syncResultTimeout = 10000;
    private static final String CONTRACT_PREFIX_FORMAT = "000000000000000000000000{}";

    // test contract info
    private static final String APP_NAME = "AppContract_{}";
    private static final String APP_METHOD_SET_PROTOCOL = "setProtocol";
    private static final String APP_METHOD_SEND_UNORDERED_MSG = "sendUnorderedMessage";
    private static final String APP_METHOD_GET_LAST_UNORDERED_MSG = "getLastUnorderedMsg";
    private static final String APP_BIN_BYTES = "608060405234801561001057600080fd5b5060006100216100c460201b60201c565b9050806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055508073ffffffffffffffffffffffffffffffffffffffff16600073ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a3506100cc565b600033905090565b6116e5806100db6000396000f3fe608060405234801561001057600080fd5b50600436106100f55760003560e01c80639670efcb11610097578063c1cecc5a11610066578063c1cecc5a14610250578063f2fde38b1461026c578063f76f703b14610288578063ff098be7146102a4576100f5565b80639670efcb146101c8578063a25ae14a146101f8578063aec2fc1414610216578063c09b261b14610234576100f5565b80633fecfe3f116100d35780633fecfe3f14610152578063715018a61461018257806389c9b1d91461018c5780638da5cb5b146101aa576100f5565b80630a9d793d146100fa57806335cd96e014610116578063387868ae14610134575b600080fd5b610114600480360381019061010f91906110ce565b6102c0565b005b61011e610380565b60405161012b9190611352565b60405180910390f35b61013c610412565b6040516101499190611337565b60405180910390f35b61016c600480360381019061016791906110f7565b610438565b6040516101799190611352565b60405180910390f35b61018a6104f1565b005b61019461062b565b6040516101a19190611352565b60405180910390f35b6101b26106bd565b6040516101bf9190611337565b60405180910390f35b6101e260048036038101906101dd91906110f7565b6106e6565b6040516101ef9190611352565b60405180910390f35b61020061079f565b60405161020d9190611352565b60405180910390f35b61021e61082d565b60405161022b9190611352565b60405180910390f35b61024e60048036038101906102499190611133565b6108bb565b005b61026a60048036038101906102659190611133565b6109f2565b005b610286600480360381019061028191906110ce565b610b13565b005b6102a2600480360381019061029d9190611133565b610cbc565b005b6102be60048036038101906102b99190611133565b610ddd565b005b6102c8610f14565b73ffffffffffffffffffffffffffffffffffffffff166102e66106bd565b73ffffffffffffffffffffffffffffffffffffffff161461033c576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016103339061142c565b60405180910390fd5b80600360006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050565b60606004805461038f906115c9565b80601f01602080910402602001604051908101604052809291908181526020018280546103bb906115c9565b80156104085780601f106103dd57610100808354040283529160200191610408565b820191906000526020600020905b8154815290600101906020018083116103eb57829003601f168201915b5050505050905090565b600360009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b6002602052816000526040600020818154811061045457600080fd5b90600052602060002001600091509150508054610470906115c9565b80601f016020809104026020016040519081016040528092919081815260200182805461049c906115c9565b80156104e95780601f106104be576101008083540402835291602001916104e9565b820191906000526020600020905b8154815290600101906020018083116104cc57829003601f168201915b505050505081565b6104f9610f14565b73ffffffffffffffffffffffffffffffffffffffff166105176106bd565b73ffffffffffffffffffffffffffffffffffffffff161461056d576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016105649061142c565b60405180910390fd5b600073ffffffffffffffffffffffffffffffffffffffff1660008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a360008060006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550565b60606005805461063a906115c9565b80601f0160208091040260200160405190810160405280929190818152602001828054610666906115c9565b80156106b35780601f10610688576101008083540402835291602001916106b3565b820191906000526020600020905b81548152906001019060200180831161069657829003601f168201915b5050505050905090565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b6001602052816000526040600020818154811061070257600080fd5b9060005260206000200160009150915050805461071e906115c9565b80601f016020809104026020016040519081016040528092919081815260200182805461074a906115c9565b80156107975780601f1061076c57610100808354040283529160200191610797565b820191906000526020600020905b81548152906001019060200180831161077a57829003601f168201915b505050505081565b600580546107ac906115c9565b80601f01602080910402602001604051908101604052809291908181526020018280546107d8906115c9565b80156108255780601f106107fa57610100808354040283529160200191610825565b820191906000526020600020905b81548152906001019060200180831161080857829003601f168201915b505050505081565b6004805461083a906115c9565b80601f0160208091040260200160405190810160405280929190818152602001828054610866906115c9565b80156108b35780601f10610888576101008083540402835291602001916108b3565b820191906000526020600020905b81548152906001019060200180831161089657829003601f168201915b505050505081565b600360009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161461094b576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016109429061144c565b60405180910390fd5b6001600083815260200190815260200160002081908060018154018082558091505060019003906000526020600020016000909190919091509080519060200190610997929190610f1c565b5080600490805190602001906109ae929190610f1c565b507f7b59e766dd02d6ce4e574f0ab75dfc4b180c90deb50dd9dbdbc65768abcf80f183838360016040516109e594939291906113b9565b60405180910390a1505050565b600360009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663c1cecc5a8484846040518463ffffffff1660e01b8152600401610a5193929190611374565b600060405180830381600087803b158015610a6b57600080fd5b505af1158015610a7f573d6000803e3d6000fd5b505050506002600083815260200190815260200160002081908060018154018082558091505060019003906000526020600020016000909190919091509080519060200190610acf929190610f1c565b507fbbe83d8459c305cf51f2425b93e693dcfaaf60f22766486b0983c905b98ead408383836000604051610b0694939291906113b9565b60405180910390a1505050565b610b1b610f14565b73ffffffffffffffffffffffffffffffffffffffff16610b396106bd565b73ffffffffffffffffffffffffffffffffffffffff1614610b8f576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610b869061142c565b60405180910390fd5b600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff161415610bff576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610bf69061140c565b60405180910390fd5b8073ffffffffffffffffffffffffffffffffffffffff1660008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a3806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050565b600360009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663f76f703b8484846040518463ffffffff1660e01b8152600401610d1b93929190611374565b600060405180830381600087803b158015610d3557600080fd5b505af1158015610d49573d6000803e3d6000fd5b505050506002600083815260200190815260200160002081908060018154018082558091505060019003906000526020600020016000909190919091509080519060200190610d99929190610f1c565b507fbbe83d8459c305cf51f2425b93e693dcfaaf60f22766486b0983c905b98ead408383836001604051610dd094939291906113b9565b60405180910390a1505050565b600360009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614610e6d576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610e649061144c565b60405180910390fd5b6001600083815260200190815260200160002081908060018154018082558091505060019003906000526020600020016000909190919091509080519060200190610eb9929190610f1c565b508060059080519060200190610ed0929190610f1c565b507f7b59e766dd02d6ce4e574f0ab75dfc4b180c90deb50dd9dbdbc65768abcf80f18383836000604051610f0794939291906113b9565b60405180910390a1505050565b600033905090565b828054610f28906115c9565b90600052602060002090601f016020900481019282610f4a5760008555610f91565b82601f10610f6357805160ff1916838001178555610f91565b82800160010185558215610f91579182015b82811115610f90578251825591602001919060010190610f75565b5b509050610f9e9190610fa2565b5090565b5b80821115610fbb576000816000905550600101610fa3565b5090565b6000610fd2610fcd8461149d565b61146c565b905082815260208101848484011115610fea57600080fd5b610ff5848285611587565b509392505050565b600061101061100b846114cd565b61146c565b90508281526020810184848401111561102857600080fd5b611033848285611587565b509392505050565b60008135905061104a8161166a565b92915050565b60008135905061105f81611681565b92915050565b600082601f83011261107657600080fd5b8135611086848260208601610fbf565b91505092915050565b600082601f8301126110a057600080fd5b81356110b0848260208601610ffd565b91505092915050565b6000813590506110c881611698565b92915050565b6000602082840312156110e057600080fd5b60006110ee8482850161103b565b91505092915050565b6000806040838503121561110a57600080fd5b600061111885828601611050565b9250506020611129858286016110b9565b9150509250929050565b60008060006060848603121561114857600080fd5b600084013567ffffffffffffffff81111561116257600080fd5b61116e8682870161108f565b935050602061117f86828701611050565b925050604084013567ffffffffffffffff81111561119c57600080fd5b6111a886828701611065565b9150509250925092565b6111bb81611535565b82525050565b6111ca81611547565b82525050565b6111d981611553565b82525050565b60006111ea826114fd565b6111f48185611513565b9350611204818560208601611596565b61120d81611659565b840191505092915050565b600061122382611508565b61122d8185611524565b935061123d818560208601611596565b61124681611659565b840191505092915050565b600061125e602683611524565b91507f4f776e61626c653a206e6577206f776e657220697320746865207a65726f206160008301527f64647265737300000000000000000000000000000000000000000000000000006020830152604082019050919050565b60006112c4602083611524565b91507f4f776e61626c653a2063616c6c6572206973206e6f7420746865206f776e65726000830152602082019050919050565b6000611304601283611524565b91507f494e56414c49445f5045524d495353494f4e00000000000000000000000000006000830152602082019050919050565b600060208201905061134c60008301846111b2565b92915050565b6000602082019050818103600083015261136c81846111df565b905092915050565b6000606082019050818103600083015261138e8186611218565b905061139d60208301856111d0565b81810360408301526113af81846111df565b9050949350505050565b600060808201905081810360008301526113d38187611218565b90506113e260208301866111d0565b81810360408301526113f481856111df565b905061140360608301846111c1565b95945050505050565b6000602082019050818103600083015261142581611251565b9050919050565b60006020820190508181036000830152611445816112b7565b9050919050565b60006020820190508181036000830152611465816112f7565b9050919050565b6000604051905081810181811067ffffffffffffffff821117156114935761149261162a565b5b8060405250919050565b600067ffffffffffffffff8211156114b8576114b761162a565b5b601f19601f8301169050602081019050919050565b600067ffffffffffffffff8211156114e8576114e761162a565b5b601f19601f8301169050602081019050919050565b600081519050919050565b600081519050919050565b600082825260208201905092915050565b600082825260208201905092915050565b60006115408261155d565b9050919050565b60008115159050919050565b6000819050919050565b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b6000819050919050565b82818337600083830152505050565b60005b838110156115b4578082015181840152602081019050611599565b838111156115c3576000848401525b50505050565b600060028204905060018216806115e157607f821691505b602082108114156115f5576115f46115fb565b5b50919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b7f4e487b7100000000000000000000000000000000000000000000000000000000600052604160045260246000fd5b6000601f19601f8301169050919050565b61167381611535565b811461167e57600080fd5b50565b61168a81611553565b811461169557600080fd5b50565b6116a18161157d565b81146116ac57600080fd5b5056fea2646970667358221220e7f9d6b5c383d5e7e73ee895955267029599208c7a63d289cc3ad8668dd70be964736f6c63430008000033";

    private static final String SDP_METHOD_GET_AM_CONTRACT = "getAmAddress";
    private static final String SDP_METHOD_GET_LOCAL_DOMAIN = "getLocalDomain";

    // test data todo: 替换为实际已注册链的信息
    private static final String senderDomain = "chainmaker20240530T3.web3";
    private static final String senderSDPAddr = "95fad2e7f7c802b3c81d93cbf306ae0920c0fb9a";
    private static final String receiverDomain = "chainmaker20240530T4.web3";
    private static final String receiverSDPAddr = "0496353f98373214f0efc809c9da67841d67f7b7";
    private static final String senderID = "senderID";
    private static final String receiverID = "receiverID";
    private static final String msg = "awesome antchain-bridge";

    private ChainMakerBBCService chainMakerBBCService;

    @Before
    public void init() {
        chainMakerBBCService = new ChainMakerBBCService();
        AbstractBBCContext mockValidCtx = mockValidCtx();
        chainMakerBBCService.startup(mockValidCtx);
    }

    @After
    public void clear() {
    }

    @Test
    public void testGetContext(){
        AbstractBBCContext ctx = chainMakerBBCService.getContext();
        Assert.assertNotNull(ctx);
        Assert.assertNull(ctx.getAuthMessageContract());
    }

    @Test
    public void testQueryLatestHeight() {
        Assert.assertNotEquals(Optional.of(0L), chainMakerBBCService.queryLatestHeight());
    }

    @Test
    public void testSetupAuthMessageContract(){
        // set up am
        chainMakerBBCService.setupAuthMessageContract();

        // get context
        AbstractBBCContext ctx = chainMakerBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
    }

    @Test
    public void testSetupSDPMessageContract(){
        // set up sdp
        chainMakerBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = chainMakerBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());
    }

    @Test
    public void testSetProtocol() {
        chainMakerBBCService.setupSDPMessageContract();
        chainMakerBBCService.setupAuthMessageContract();

        AbstractBBCContext ctx = chainMakerBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        chainMakerBBCService.setProtocol(ctx.getSdpContract().getContractAddress(), "0");

        ctx = chainMakerBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getAuthMessageContract().getStatus());
    }

    @Test
    public void testSetAmContractAndLocalDomain() {
        chainMakerBBCService.setupSDPMessageContract();
        chainMakerBBCService.setupAuthMessageContract();

        AbstractBBCContext ctx = chainMakerBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        chainMakerBBCService.setAmContract(ctx.getAuthMessageContract().getContractAddress());
        ctx = chainMakerBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        chainMakerBBCService.setLocalDomain("receiverDomain");
        ctx = chainMakerBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getSdpContract().getStatus());
    }

    @Test
    public void testQuerySDPMessageSeq(){
        prepare();

        long seq = chainMakerBBCService.querySDPMessageSeq(
                senderDomain,
                DigestUtil.sha256Hex(senderID),
                receiverDomain,
                DigestUtil.sha256Hex(receiverID)
        );
        Assert.assertEquals(0L, seq);
    }

    @Test
    public void testRelayAuthMessage() throws Exception {
        prepare();

        // relay am msg
        CrossChainMessageReceipt receipt = chainMakerBBCService.relayAuthMessage(getRawMsgFromRelayer());
        Assert.assertTrue(receipt.isSuccessful());

        System.out.println("sleep 10s for tx to be packaged...");
        Thread.sleep(10000);

        ChainmakerTransaction.TransactionInfo transactionInfo =
                chainMakerBBCService.getChainClient().getTxByTxId(receipt.getTxhash(), rpcCallTimeout);
        System.out.println(receipt.getTxhash());
        System.out.println(transactionInfo.toString());
        Assert.assertNotNull(transactionInfo);
    }

    @Test
    public void testReadCrossChainMessageReceipt() throws IOException, InterruptedException {
        prepare();

        // relay am msg
        CrossChainMessageReceipt receipt = chainMakerBBCService.relayAuthMessage(getRawMsgFromRelayer());

        System.out.println("sleep 10s for tx to be packaged...");
        Thread.sleep(10000);

        // read receipt by txHash
        CrossChainMessageReceipt receipt1 = chainMakerBBCService.readCrossChainMessageReceipt(receipt.getTxhash());
        System.out.println(receipt.getTxhash());
        Assert.assertTrue(receipt1.isConfirmed());
        Assert.assertEquals(receipt.isSuccessful(), receipt1.isSuccessful());
    }

    @Test
    public void testReadCrossChainMessagesByHeight_sendUnordered() throws Exception {

        try {
            ResultOuterClass.TxResponse responseInfo;

            // 0-1. check sdp
            responseInfo = chainMakerBBCService.invokeContract(
                    senderSDPAddr,   // demo合约
                    SDP_METHOD_GET_AM_CONTRACT,      // function name
                    Arrays.<Type>asList(),
                    Collections.<TypeReference<?>>emptyList()
            );
            Assert.assertEquals(ResultOuterClass.TxStatusCode.SUCCESS, responseInfo.getCode());

            // 0-2. check sdp
            responseInfo = chainMakerBBCService.invokeContract(
                    senderSDPAddr,   // demo合约
                    SDP_METHOD_GET_LOCAL_DOMAIN,      // function name
                    Arrays.<Type>asList(),
                    Collections.<TypeReference<?>>emptyList()
            );
            Assert.assertEquals(ResultOuterClass.TxStatusCode.SUCCESS, responseInfo.getCode());

            // 1. sender deploy sender contract
            String senderAppContractName = Utils.calcContractName(StrUtil.format(APP_NAME, UUID.randomUUID().toString()));
            ContractOuterClass.Contract senderAppContract = chainMakerBBCService.setupContract(APP_BIN_BYTES, senderAppContractName);
            String senderAppContractAddr = senderAppContract.getAddress();

            // 2. sender set protocol
            responseInfo = chainMakerBBCService.invokeContract(
                    senderAppContractAddr,   // demo合约
                    APP_METHOD_SET_PROTOCOL,      // function name
                    Arrays.<Type>asList(new Address(senderSDPAddr)),
                    Collections.<TypeReference<?>>emptyList()
            );
            Assert.assertEquals(ResultOuterClass.TxStatusCode.SUCCESS, responseInfo.getCode());

            // 3. receiver deploy receiver contract
            String receiverAppContractName = Utils.calcContractName(StrUtil.format(APP_NAME, UUID.randomUUID().toString()));
            ContractOuterClass.Contract receiverAppContract = chainMakerBBCService.setupContract(APP_BIN_BYTES, receiverAppContractName);
            String receiverAppContractAddr = receiverAppContract.getAddress();

            // 4. receiver set protocol
            responseInfo = chainMakerBBCService.invokeContract(
                    receiverAppContractAddr,   // demo合约
                    APP_METHOD_SET_PROTOCOL,      // function name
                    Arrays.<Type>asList(new Address(receiverSDPAddr)),
                    Collections.<TypeReference<?>>emptyList()
            );
            Assert.assertEquals(ResultOuterClass.TxStatusCode.SUCCESS, responseInfo.getCode());

            Thread.sleep(10000);
            System.out.printf("wait for add acl, grantDomain: %s, grandIdentity: %s, ownerDomain: %s, ownerIdentity: %s\n",
                    senderDomain,
                    senderAppContractAddr,
                    receiverDomain,
                    receiverAppContractAddr);

            // 5. send msg
            responseInfo = chainMakerBBCService.invokeContract(
                    senderAppContractAddr,   // demo合约
                    APP_METHOD_SEND_UNORDERED_MSG,      // function name
                    Arrays.<Type>asList(
                            new Utf8String(receiverDomain),
                            new Bytes32(str2bytes32(StrUtil.format(CONTRACT_PREFIX_FORMAT, receiverAppContractAddr))),
                            new DynamicBytes(msg.getBytes())),
                    Collections.<TypeReference<?>>emptyList()
            );

            Assert.assertEquals(ResultOuterClass.TxStatusCode.SUCCESS, responseInfo.getCode());
            System.out.println("responseInfo\n" + responseInfo.toString());

            Thread.sleep(10000);

            // 6. get msg
            responseInfo = chainMakerBBCService.invokeContract(
                    receiverAppContractAddr,   // demo合约
                    APP_METHOD_GET_LAST_UNORDERED_MSG,      // function name
                    Arrays.<Type>asList(),
                    Collections.<TypeReference<?>>emptyList()
            );

            Assert.assertEquals(ResultOuterClass.TxStatusCode.SUCCESS, responseInfo.getCode());
            Assert.assertTrue(new String(responseInfo.getContractResult().getResult().toByteArray()).contains(msg));
            System.out.println("responseInfo\n" + new String(responseInfo.getContractResult().getResult().toByteArray()));

        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format("failed to send unordered msg"), e
            );
        }
    }

    public byte[] str2bytes32(String str) {
        if (str.length() != 64) {
            throw new RuntimeException("str {} length is not 64");
        }
        byte[] byteArray = new byte[32];
        for (int i = 0; i < byteArray.length; i++) {
            int index = i * 2;
            // 将每两个十六进制字符转换为一个 byte
            int value = Integer.parseInt(str.substring(index, index + 2), 16);
            byteArray[i] = (byte) value;
        }
        return byteArray;
    }

    private AbstractBBCContext mockValidCtx() {
        ChainMakerConfig mockConf = new ChainMakerConfig();
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        try {
            String jsonString = readFileJson(SDK_CONFIG_JSON);
            mockConf = ChainMakerConfig.fromJsonString(jsonString);

            mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
            return mockCtx;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mockCtx;
    }

    private void prepare(){
        chainMakerBBCService.setupSDPMessageContract();
        chainMakerBBCService.setupAuthMessageContract();

        chainMakerBBCService.setProtocol(chainMakerBBCService.getContext().getSdpContract().getContractAddress(), "0");
        chainMakerBBCService.setLocalDomain(receiverDomain);
        chainMakerBBCService.setAmContract(chainMakerBBCService.getContext().getAuthMessageContract().getContractAddress());

        AbstractBBCContext ctx = chainMakerBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getSdpContract().getStatus());
    }

    // return a byte[] containing rawProof and its length, used to send msg in relayers
    private byte[] getRawMsgFromRelayer() throws IOException {
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(
                1,
                "SDPMessageV2Used".getBytes(),
                "receiverDomain",
                HexUtil.decodeHex(
                        StrUtil.format("000000000000000000000000{}", receiverID)
                ),
                -1,
                "awesome antchain-bridge".getBytes()
        );

        IAuthMessage am = AuthMessageFactory.createAuthMessage(
                1,
                DigestUtil.sha256("senderID"),
                0,
                sdpMessage.encode()
        );

        MockResp resp = new MockResp();
        resp.setRawResponse(am.encode());

        MockProof proof = new MockProof();
        proof.setResp(resp);
        proof.setDomain("senderDomain");

        byte[] rawProof = TLVUtils.encode(proof);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(new byte[]{0, 0, 0, 0});

        int len = rawProof.length;
        stream.write((len >>> 24) & 0xFF);
        stream.write((len >>> 16) & 0xFF);
        stream.write((len >>> 8) & 0xFF);
        stream.write((len) & 0xFF);

        stream.write(rawProof);

        return stream.toByteArray();
    }

    @Getter
    @Setter
    public static class MockProof {

        @TLVField(tag = 5, type = TLVTypeEnum.BYTES)
        private MockResp resp;

        @TLVField(tag = 9, type = TLVTypeEnum.STRING)
        private String domain;
    }

    @Getter
    @Setter
    public static class MockResp {

        @TLVField(tag = 0, type = TLVTypeEnum.BYTES)
        private byte[] rawResponse;
    }

    public static String readFileJson(String fileName) {
        StringBuilder jsonStringBuilder = new StringBuilder();

        try {
            // 使用ClassLoader获取资源文件的输入流
            InputStream inputStream = ChainMakerConfig.class.getClassLoader().getResourceAsStream(fileName);

            // 使用BufferedReader逐行读取文件内容
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }

            // 关闭资源
            reader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return jsonStringBuilder.toString();
    }
}