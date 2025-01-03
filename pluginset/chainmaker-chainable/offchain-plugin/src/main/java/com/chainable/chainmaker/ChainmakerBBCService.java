/*
 * Copyright 2024 Chainable
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

package com.chainable.chainmaker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.plugins.lib.BBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import org.chainmaker.pb.common.ChainmakerBlock;
import org.chainmaker.pb.common.ChainmakerTransaction;
import org.chainmaker.pb.common.ChainmakerTransaction.TransactionInfo;
import org.chainmaker.pb.common.ResultOuterClass;
import org.web3j.utils.Numeric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.web3j.abi.*;
import org.web3j.abi.datatypes.*;


@BBCService(products = "chainmaker", pluginId = "chainmaker_bbcservice")
public class ChainmakerBBCService implements IBBCService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChainmakerBBCService.class);

    private static final String SEND_AUTH_MESSAGE_NAME = "SendAuthMessage";
    public static final Event SEND_AUTH_MESSAGE_EVENT = new Event(SEND_AUTH_MESSAGE_NAME,
            Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {
            }));
    private static final String SEND_AUTH_MESSAGE_SIGNATURE = EventEncoder.encode(SEND_AUTH_MESSAGE_EVENT);

    private ChainmakerSDK sdk;

    private AbstractBBCContext bbcContext;

    @Override
    public void startup(AbstractBBCContext abstractBBCContext) {
        try {
            this.sdk = new ChainmakerSDK();
            this.sdk.initSDK(new byte[]{});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.bbcContext = abstractBBCContext;
    }

    @Override
    public void shutdown() {
        LOGGER.info("shut down service");
        sdk.shutdown();
    }

    @Override
    public AbstractBBCContext getContext() {
        LOGGER.info("[getContext] context is: {}===========================\n am: {}\n, sdp: {}\n",
                this.bbcContext.toString(),
                this.bbcContext.getAuthMessageContract().getContractAddress(),
                this.bbcContext.getSdpContract().getContractAddress());
        return this.bbcContext;
    }

    @Override
    public void setupAuthMessageContract() {
        AuthMessageContract am = new AuthMessageContract();
        am.setContractAddress(sdk.getAmContractAddress());
        am.setStatus(ContractStatusEnum.CONTRACT_READY);

        this.bbcContext.setAuthMessageContract(am);

        LOGGER.info("set up am contract");
    }

    @Override
    public void setupSDPMessageContract() {
        SDPContract sdpContract = new SDPContract();
        sdpContract.setContractAddress(sdk.getSdpContractAddress());
        sdpContract.setStatus(ContractStatusEnum.CONTRACT_READY);

        this.bbcContext.setSdpContract(sdpContract);

        LOGGER.info("set up sdp contract");
    }

    @Override
    public void setProtocol(String protocolAddress, String protocolType) {
        this.sdk.setProtocol(this.bbcContext.getAuthMessageContract().getContractAddress(),
                "setProtocol",
                protocolAddress, protocolType
        );
        LOGGER.info("set protocol");
    }

    @Override
    public void setAmContract(String contractAddress) {
        this.sdk.setAmContract(this.bbcContext.getSdpContract().getContractAddress(),
                "setAmContract",
                contractAddress
        );
        LOGGER.info("set am contract");
    }

    @Override
    public void setLocalDomain(String domain) {
        this.sdk.setLocalDomain(this.bbcContext.getSdpContract().getContractAddress(),
                "setLocalDomain",
                domain
        );
        LOGGER.info("set local domain " + domain);
    }

    @Override
    public CrossChainMessageReceipt readCrossChainMessageReceipt(String txhash) {
        TransactionInfo transaction = this.sdk.queryTx(txhash);

        CrossChainMessageReceipt crossChainMessageReceipt = buildCrossChainMessageReceipt(txhash, transaction);

        LOGGER.info("read crosschain message receipt [txhash: "+crossChainMessageReceipt.getTxhash()+", isConfirmed: "+crossChainMessageReceipt.isConfirmed()+", isSuccessful: "+crossChainMessageReceipt.isSuccessful()+", ErrorMsg: {}",crossChainMessageReceipt.getErrorMsg());
        return crossChainMessageReceipt;
    }

    private static CrossChainMessageReceipt buildCrossChainMessageReceipt(String txhash, TransactionInfo tx) {
        CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();
        crossChainMessageReceipt.setConfirmed(tx.hasTransaction() && tx.getTransaction().hasResult());
        crossChainMessageReceipt.setSuccessful(tx.getTransaction().getResult().getCode().getNumber() == ResultOuterClass.TxStatusCode.SUCCESS_VALUE);
        crossChainMessageReceipt.setTxhash(txhash);
        crossChainMessageReceipt.setErrorMsg("");
        return crossChainMessageReceipt;
    }

    @Override
    public List<CrossChainMessage> readCrossChainMessagesByHeight(long l) {
        ChainmakerBlock.Block block = this.sdk.queryABlock(l).getBlock();
        ChainmakerBlock.BlockHeader blockHeader = block.getHeader();

        List<ChainmakerTransaction.Transaction> txs = block.getTxsList();
        String AuthMessageContractName = this.bbcContext.getAuthMessageContract().getContractAddress();

        return txs.stream().flatMap(tx -> {
            if (!tx.hasResult() || !tx.getResult().hasContractResult() || tx.getResult().getContractResult().getContractEventCount() == 0) {
                return Stream.empty();
            } else {
                List<CrossChainMessage> messages = new ArrayList<>();
                for (ResultOuterClass.ContractEvent event : tx.getResult().getContractResult().getContractEventList()) {
                    if (event.getContractName().equals(AuthMessageContractName) && event.getTopic().equals(SEND_AUTH_MESSAGE_SIGNATURE.replace("0x", ""))) {
                        messages.add(CrossChainMessage.createCrossChainMessage(
                                CrossChainMessage.CrossChainMessageType.AUTH_MSG,
                                blockHeader.getBlockHeight(),
                                blockHeader.getBlockTimestamp(),
                                blockHeader.getBlockHash().toByteArray(),
                                // this is very important to put the auth-message inside
                                (byte[]) FunctionReturnDecoder.decode(event.getEventData(0), SEND_AUTH_MESSAGE_EVENT.getNonIndexedParameters()).get(0).getValue(),
                                // put the ledger data inside, just for SPV or other attestations
                                tx.getResult().toByteArray(),
                                // this time we need no proof data. it's ok to set it with empty bytes
                                "".getBytes(),
                                HexUtil.decodeHex(tx.getPayload().getTxId())
                        ));
                    }
                }
                return messages.stream();
            }
        }).collect(Collectors.toList());
    }

    @Override
    public long querySDPMessageSeq(String senderDomain, String fromAddress, String receiverDomain, String toAddress) {
        ResultOuterClass.TxResponse txResponse = this.sdk.querySDPMessageSeq(
                this.bbcContext.getSdpContract().getContractAddress(),
                "querySDPMessageSeq", senderDomain, fromAddress, receiverDomain, toAddress
        );
        return Numeric.toBigInt(txResponse.getContractResult().toByteArray()).longValue();
    }

    @Override
    public CrossChainMessageReceipt relayAuthMessage(byte[] bytes) {
        // call AM contract to commit the AuthMessage.
        ResultOuterClass.TxResponse txResponse = this.sdk.relayAuthMessage(
                this.bbcContext.getAuthMessageContract().getContractAddress(),
                "recvPkgFromRelayer",
                bytes
        );

        // collect receipt information to fill all fields in the CrossChainReceipt
        CrossChainMessageReceipt ret = new CrossChainMessageReceipt();
        ret.setTxhash(txResponse.getTxId());
        ret.setConfirmed(txResponse.hasContractResult());
        ret.setSuccessful(txResponse.getContractResult().getCode() == ResultOuterClass.TxStatusCode.SUCCESS_VALUE);
        ret.setErrorMsg("");

        LOGGER.info("crosschain msg receipt [txhash: "+ret.getTxhash()+", isConfirmed: "+ret.isConfirmed()+", isSuccessful: "+ret.isSuccessful()+", ErrorMsg: "+ret.getErrorMsg());
        return ret;
    }

    @Override
    public Long queryLatestHeight() {
        return this.sdk.queryLatestHeight();
    }
}
