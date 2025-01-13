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

package com.alipay.antchain.bridge.plugins.demo.testchain;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.bbc.syscontract.PTCContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.commons.core.base.*;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import com.alipay.antchain.bridge.commons.core.ptc.PTCVerifyAnchor;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyBlockchainTrustAnchor;
import com.alipay.antchain.bridge.plugins.lib.BBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;

/**
 * This {@code TestChainBBCService} is a demo implementation of {@link IBBCService}.
 * <p>
 * It's a <b>{@code Blockchain Bridge Component}</b> plugin for the heterogeneous blockchain <b>testchain</b>
 * in this demo.
 * </p>
 */
@BBCService(products = "testchain", pluginId = "testchain_bbcservice")
public class TestChainBBCService extends AbstractBBCService {

    private TestChainSDK sdk;

    private AbstractBBCContext bbcContext;

    @Override
    public void startup(AbstractBBCContext abstractBBCContext) {
        getBBCLogger().info("start up service");
        getBBCLogger().info("context is {}", JSON.toJSONString(abstractBBCContext));

        this.sdk = new TestChainSDK();
        this.sdk.initSDK(abstractBBCContext.getConfForBlockchainClient());
        this.bbcContext = abstractBBCContext;
    }

    @Override
    public void shutdown() {
        getBBCLogger().info("shut down service");
        sdk.shutdown();
    }

    @Override
    public AbstractBBCContext getContext() {
        return this.bbcContext;
    }

    @Override
    public void setupAuthMessageContract() {

        // Pretend that we deploy AuthMessage contract.
        // And add the relayer address to the AuthMessage contract
        // by calling the `addRelayer`. If you don't want to check
        // the relayer address, just remove the related code from contract.
        AuthMessageContract am = new AuthMessageContract();
        am.setContractAddress("am");
        am.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);

        // Then set contract to context.
        this.bbcContext.setAuthMessageContract(am);

        getBBCLogger().info("set up am contract");
    }

    @Override
    public void setupSDPMessageContract() {
        // Pretend that we deploy SDP contract.
        SDPContract sdpContract = new SDPContract();
        sdpContract.setContractAddress("sdp");
        sdpContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);

        // Then set contract to context.
        this.bbcContext.setSdpContract(sdpContract);

        getBBCLogger().info("set up sdp contract");
    }

    @Override
    public void setupPTCContract() {
        // Pretend that we deploy PTC contract.
        PTCContract ptcContract = new PTCContract();
        ptcContract.setContractAddress("ptc");
        ptcContract.setStatus(ContractStatusEnum.CONTRACT_READY);

        // Then set contract to context.
        this.bbcContext.setPtcContract(ptcContract);

        getBBCLogger().info("set up ptc contract");
    }

    @Override
    public void setProtocol(String protocolAddress, String protocolType) {
        this.sdk.syncCallContract(
                this.bbcContext.getAuthMessageContract().getContractAddress(),
                "setProtocol",
                ListUtil.toList(protocolAddress, protocolType)
        );
        this.bbcContext.getAuthMessageContract().setStatus(ContractStatusEnum.CONTRACT_READY);
        getBBCLogger().info("set protocol");
    }

    @Override
    public void setPtcContract(String ptcContractAddress) {
       this.sdk.syncCallContract(
                this.bbcContext.getAuthMessageContract().getContractAddress(),
                "setPtcContract",
                ListUtil.toList(ptcContractAddress)
        );
        // make sure both `setPtcContract` and `setLocalDomain` has been called successfully
        this.bbcContext.getAuthMessageContract().setStatus(ContractStatusEnum.CONTRACT_READY);
        getBBCLogger().info("set ptc contract");
    }

    @Override
    public void setAmContract(String contractAddress) {
        this.sdk.syncCallContract(
                this.bbcContext.getSdpContract().getContractAddress(),
                "setAmContract",
                ListUtil.toList(contractAddress)
        );
        // make sure both `setAmContract` and `setLocalDomain` has been called successfully
        this.bbcContext.getSdpContract().setStatus(ContractStatusEnum.CONTRACT_READY);
        getBBCLogger().info("set am contract");
    }

    @Override
    public void setLocalDomain(String domain) {
        this.sdk.syncCallContract(
                this.bbcContext.getSdpContract().getContractAddress(),
                "setLocalDomain",
                ListUtil.toList(domain)
        );
        // make sure both `setAmContract` and `setLocalDomain` has been called successfully
        this.bbcContext.getSdpContract().setStatus(ContractStatusEnum.CONTRACT_READY);
        getBBCLogger().info("set local domain {}", domain);
    }

    @Override
    public CrossChainMessageReceipt readCrossChainMessageReceipt(String txhash) {
        TestChainSDK.TestChainTransaction transaction = this.sdk.queryTx(txhash);

        CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();
        crossChainMessageReceipt.setConfirmed(transaction.isConfirmed());
        crossChainMessageReceipt.setSuccessful(transaction.isSuccessToExecute());
        crossChainMessageReceipt.setTxhash(txhash);
        crossChainMessageReceipt.setErrorMsg("");

        getBBCLogger().info("read crosschain message receipt [txhash: {}, isConfirmed: {}, isSuccessful: {}, ErrorMsg: {}",
                crossChainMessageReceipt.getTxhash(), crossChainMessageReceipt.isConfirmed(), crossChainMessageReceipt.isSuccessful(), crossChainMessageReceipt.getErrorMsg());

        return crossChainMessageReceipt;
    }

    @Override
    public List<CrossChainMessage> readCrossChainMessagesByHeight(long l) {
        TestChainSDK.TestChainBlock block = this.sdk.queryABlock(l);

        getBBCLogger().info("read cross-chain msg by height");

        return block.getReceipts().stream().filter(
                // find all logs generated by AM contract
                testChainReceipt -> StrUtil.equals(
                        testChainReceipt.getContract(),
                        this.bbcContext.getAuthMessageContract().getContractAddress()
                )
        ).filter(
                // filter the logs with topic 'SendAuthMessage'
                // which contains the sending auth message.
                testChainReceipt -> StrUtil.equals(testChainReceipt.getTopic(), "SendAuthMessage")
        ).map(
                testChainReceipt -> CrossChainMessage.createCrossChainMessage(
                        CrossChainMessage.CrossChainMessageType.AUTH_MSG,
                        block.getHeight(),
                        block.getTimestamp(),
                        block.getBlockHash(),
                        // this is very important to put the auth-message inside
                        HexUtil.decodeHex(testChainReceipt.getLogValue()),
                        // put the ledger data inside, just for SPV or other attestations
                        testChainReceipt.toBytes(),
                        // this time we need no proof data. it's ok to set it with empty bytes
                        "pretend that we have merkle proof or some stuff".getBytes(),
                        testChainReceipt.getTxhash().getBytes()
                )
        ).collect(Collectors.toList());
    }

    @Override
    public ConsensusState readConsensusState(BigInteger height) {
        return new ConsensusState(
                height,
                RandomUtil.randomBytes(32),
                RandomUtil.randomBytes(32),
                DateUtil.current(),
                "header".getBytes(),
                "consensus pubkeys".getBytes(),
                "consensus sigs".getBytes()
        );
    }

    @Override
    public long querySDPMessageSeq(String senderDomain, String fromAddress, String receiverDomain, String toAddress) {
        TestChainSDK.TestChainReceipt receipt = this.sdk.syncCallContract(
                this.bbcContext.getSdpContract().getContractAddress(),
                "querySDPMessageSeq",
                ListUtil.toList(senderDomain, fromAddress, receiverDomain, toAddress)
        );

        getBBCLogger().info("query sdp msg seq");
        return Long.parseLong((String) receipt.getResult());
    }

    @Override
    public CrossChainMessageReceipt relayAuthMessage(byte[] bytes) {
        // call AM contract to commit the AuthMessage.
        TestChainSDK.TestChainReceipt receipt = this.sdk.syncCallContract(
                this.bbcContext.getAuthMessageContract().getContractAddress(),
                "recvPkgFromRelayer",
                ListUtil.toList(HexUtil.encodeHexStr(bytes))
        );
        // call asyncCallContract
//        TestChainSDK.TestChainReceipt receipt = this.sdk.asyncCallContract(
//                this.bbcContext.getAuthMessageContract().getContractAddress(),
//                "recvPkgFromRelayer",
//                ListUtil.toList(HexUtil.encodeHexStr(bytes))
//        );

        // collect receipt information to fill all fields in the CrossChainReceipt
        CrossChainMessageReceipt ret = new CrossChainMessageReceipt();
        ret.setTxhash(receipt.getTxhash());
        ret.setConfirmed(true);
        ret.setSuccessful(true);
        ret.setErrorMsg("");

        getBBCLogger().info("crosschain msg receipt [txhash: {}, isConfirmed: {}, isSuccessful: {}, ErrorMsg: {}",
                ret.getTxhash(), ret.isConfirmed(), ret.isSuccessful(), ret.getErrorMsg());
        return ret;
    }

    @Override
    public Long queryLatestHeight() {
        getBBCLogger().info("query the latest height");
        return this.sdk.queryLatestHeight();
    }

    @Override
    public void updatePTCTrustRoot(PTCTrustRoot ptcTrustRoot) {

    }

    @Override
    public boolean hasTpBta(CrossChainLane tpbtaLane, int tpBtaVersion) {
        return true;
    }

    @Override
    public ThirdPartyBlockchainTrustAnchor getTpBta(CrossChainLane tpbtaLane, int tpBtaVersion) {
        return null;
    }

    @Override
    public void addTpBta(ThirdPartyBlockchainTrustAnchor tpbta) {

    }

    @Override
    public Set<PTCTypeEnum> getSupportedPTCType() {
        return new HashSet<>();
    }

    @Override
    public PTCTrustRoot getPTCTrustRoot(ObjectIdentity ptcOwnerOid) {
        return new PTCTrustRoot();
    }

    @Override
    public boolean hasPTCVerifyAnchor(ObjectIdentity ptcOwnerOid, BigInteger version) {
        return true;
    }

    @Override
    public boolean hasPTCTrustRoot(ObjectIdentity ptcOwnerOid) {
        return false;
    }

    @Override
    public PTCVerifyAnchor getPTCVerifyAnchor(ObjectIdentity ptcOwnerOid, BigInteger version) {
        return null;
    }
}
