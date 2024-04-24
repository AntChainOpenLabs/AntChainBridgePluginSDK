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
package com.alipay.antchain.bridge.plugins.fiscobcos;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.plugins.lib.BBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import lombok.Getter;
import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosBlock;
import org.fisco.bcos.sdk.v3.client.protocol.response.BcosTransactionReceipt;
import org.fisco.bcos.sdk.v3.client.protocol.response.BlockNumber;
import org.fisco.bcos.sdk.v3.codec.ContractCodecException;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import com.alipay.antchain.bridge.plugins.fiscobcos.abi.AuthMsg;
import com.alipay.antchain.bridge.plugins.fiscobcos.abi.SDPMsg;
import org.fisco.bcos.sdk.v3.eventsub.EventSubParams;
import org.fisco.bcos.sdk.v3.eventsub.EventSubscribe;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.model.callback.TransactionCallback;
import org.fisco.bcos.sdk.v3.transaction.codec.decode.TransactionDecoderInterface;
import org.fisco.bcos.sdk.v3.transaction.codec.decode.TransactionDecoderService;
import org.fisco.bcos.sdk.v3.transaction.manager.AssembleTransactionProcessor;
import org.fisco.bcos.sdk.v3.transaction.manager.TransactionProcessorFactory;
import org.fisco.bcos.sdk.v3.transaction.model.dto.CallResponse;
import org.fisco.bcos.sdk.v3.transaction.model.dto.TransactionResponse;
import org.fisco.bcos.sdk.v3.codec.abi.tools.TopicTools;
import org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes;
import org.fisco.bcos.sdk.v3.transaction.tools.ContractLoader;

import static com.alipay.antchain.bridge.plugins.fiscobcos.abi.AuthMsg.SENDAUTHMESSAGE_EVENT;

@BBCService(products = "fiscobcos", pluginId = "plugin-fiscobcos")
@Getter
public class FISCOBCOSBBCService extends AbstractBBCService{
    private FISCOBCOSConfig config;

    private BcosSDK sdk;
    private Client client;
    private CryptoKeyPair keyPair;
    private AssembleTransactionProcessor transactionProcessor;

    private AbstractBBCContext bbcContext;
    private static final String abiFile = FISCOBCOSBBCService.class.getClassLoader().getResource("abi").getPath();
    private static final String binFile = FISCOBCOSBBCService.class.getClassLoader().getResource("bin").getPath();

    @Override
    public void startup(AbstractBBCContext abstractBBCContext) {

    }

    @Override
    public void shutdown() {
    }

    @Override
    public AbstractBBCContext getContext() {
        return null;
    }

    @Override
    public CrossChainMessageReceipt readCrossChainMessageReceipt(String txHash) {
        return null;
    }

    private CrossChainMessageReceipt getCrossChainMessageReceipt(TransactionReceipt transactionReceipt) {
        return null;
    }

    @Override
    public List<CrossChainMessage> readCrossChainMessagesByHeight(long height) {return null;}

    @Override
    public Long queryLatestHeight() {return null;}

    @Override
    public long querySDPMessageSeq(String senderDomain, String senderID, String receiverDomain, String receiverID) {
        return 0;
    }

    @Override
    public void setProtocol(String protocolAddress, String protocolType) {return;}

    @Override
    public CrossChainMessageReceipt relayAuthMessage(byte[] rawMessage) {return null;}

    @Override
    public void setupAuthMessageContract() {return;}

    @Override
    public void setupSDPMessageContract() {
        return;
    }

    @Override
    public void setAmContract(String contractAddress) {
       return;
    }


    @Override
    public void setLocalDomain(String domain) {
        return;
    }
}