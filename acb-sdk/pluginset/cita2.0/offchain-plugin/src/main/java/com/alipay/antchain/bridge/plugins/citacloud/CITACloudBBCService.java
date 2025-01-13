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

package com.alipay.antchain.bridge.plugins.citacloud;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.plugins.citacloud.client.CITACloudAutoRefreshClient;
import com.alipay.antchain.bridge.plugins.citacloud.types.CITACloudLogInfo;
import com.alipay.antchain.bridge.plugins.lib.BBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import com.cita.cloud.response.TransactionReceipt;
import com.citahub.cita.abi.TypeReference;
import com.citahub.cita.abi.datatypes.Address;
import com.citahub.cita.abi.datatypes.DynamicBytes;
import com.citahub.cita.abi.datatypes.Type;
import com.citahub.cita.abi.datatypes.Utf8String;
import com.citahub.cita.abi.datatypes.generated.Bytes32;
import com.citahub.cita.abi.datatypes.generated.Uint32;
import com.citahub.cita.utils.Numeric;

@BBCService(pluginId = "citacloud-plugin", products = "citacloud")
public class CITACloudBBCService implements IBBCService {

    private static final String CROSS_CHAIN_EVENT_METHOD = "SendAuthMessage(bytes)";

    private static final String FUNC_NAME_QUERY_SDP_MESSAGE_SEQ = "querySDPMessageSeq";

    private static final String FUNC_NAME_SET_PROTOCOL = "setProtocol";

    private static final String FUNC_NAME_SET_AM_CONTRACT = "setAmContract";

    private static final String FUNC_NAME_SET_LOCAL_DOMAIN = "setLocalDomain";

    private static final String FUNC_NAME_RECV_PKG_FROM_RELAYER = "recvPkgFromRelayer";

    private static final String FUNC_NAME_GET_AM_ADDRESS = "getAmAddress";

    private static final String FUNC_NAME_GET_LOCAL_DOMAIN = "getLocalDomain";

    private static final String FUNC_NAME_GET_PROTOCOL = "getProtocol";

    private CITACloudAutoRefreshClient citaCloudClient;

    private CITACloudConfig config;

    private AbstractBBCContext bbcContext;

    @Override
    public void startup(AbstractBBCContext context) {
        this.bbcContext = context;
        this.config = CITACloudConfig.parseFrom(context.getConfForBlockchainClient());
        try {
            citaCloudClient = new CITACloudAutoRefreshClient(this.config);
            citaCloudClient.init();
        } catch (Exception e) {
            throw new RuntimeException("failed to init CITA client: ", e);
        }

        if (ObjectUtil.isNull(context.getAuthMessageContract())
                && StrUtil.isNotEmpty(this.config.getAmContractAddressDeployed())) {
            AuthMessageContract authMessageContract = new AuthMessageContract();
            authMessageContract.setContractAddress(this.config.getAmContractAddressDeployed());
            authMessageContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
            this.bbcContext.setAuthMessageContract(authMessageContract);
        }

        if (ObjectUtil.isNull(context.getSdpContract())
                && StrUtil.isNotEmpty(this.config.getSdpContractAddressDeployed())) {
            SDPContract sdpContract = new SDPContract();
            sdpContract.setContractAddress(this.config.getSdpContractAddressDeployed());
            sdpContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
            this.bbcContext.setSdpContract(sdpContract);
        }
    }

    @Override
    public void shutdown() {
        this.citaCloudClient.shutdown();
    }

    @Override
    public AbstractBBCContext getContext() {
        return this.bbcContext;
    }

    @Override
    public CrossChainMessageReceipt readCrossChainMessageReceipt(String txhash) {
        TransactionReceipt receipt = this.citaCloudClient.queryTxInfo(txhash);
        if (ObjectUtil.isNull(receipt)) {
            throw new RuntimeException("empty transaction receipt for tx " + txhash);
        }

        CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();
        crossChainMessageReceipt.setSuccessful(citaCloudClient.isTxExecuteSuccess(receipt));
        crossChainMessageReceipt.setConfirmed(
                ObjectUtil.isNotNull(receipt.getBlockNumber()) && receipt.getBlockNumber().longValue() > 0
        );
        crossChainMessageReceipt.setTxhash(txhash);
        crossChainMessageReceipt.setErrorMsg(StrUtil.emptyToDefault(receipt.getErrorMessage(), ""));

        return crossChainMessageReceipt;
    }

    @Override
    public List<CrossChainMessage> readCrossChainMessagesByHeight(long height) {
        List<CITACloudLogInfo> logs = this.citaCloudClient.filterContractEventsByHeight(
                height,
                this.bbcContext.getAuthMessageContract().getContractAddress(),
                CROSS_CHAIN_EVENT_METHOD
        );
        if (ObjectUtil.isEmpty(logs)) {
            return ListUtil.empty();
        }
        return logs.stream().map(
                log -> CrossChainMessage.createCrossChainMessage(
                        CrossChainMessage.CrossChainMessageType.AUTH_MSG,
                        height,
                        log.getTimestamp(),
                        Numeric.hexStringToByteArray(log.getLog().getBlockHash()),
                        parseAMPkgFromEvmEventData(Numeric.hexStringToByteArray(log.getLog().getData())),
                        new byte[]{},
                        new byte[]{},
                        HexUtil.decodeHex(log.getLog().getTransactionHash().replaceFirst("^0x", ""))
                )
        ).collect(Collectors.toList());
    }

    private byte[] parseAMPkgFromEvmEventData(byte[] rawEventData) {
        int pkgLen = Numeric.toBigInt(ArrayUtil.sub(rawEventData, 32, 64)).intValue();
        if (pkgLen > rawEventData.length - 64) {
            throw new RuntimeException("wrong length of event SendAuthMessage's pkg");
        }
        return ArrayUtil.sub(rawEventData, 64, pkgLen + 64);
    }

    @Override
    public Long queryLatestHeight() {
        return this.citaCloudClient.queryLatestHeight();
    }

    @Override
    public long querySDPMessageSeq(String senderDomain, String fromAddress, String receiverDomain, String toAddress) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getSdpContract())) {
            throw new RuntimeException("empty sdp contract in bbc context");
        }

        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Utf8String(senderDomain));
        inputParameters.add(new Bytes32(Numeric.hexStringToByteArray(fromAddress)));
        inputParameters.add(new Utf8String(receiverDomain));
        inputParameters.add(new Bytes32(Numeric.hexStringToByteArray(toAddress)));

        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(TypeReference.create(Uint32.class));

        List<Type> result = this.citaCloudClient.localCallContract(
                this.bbcContext.getSdpContract().getContractAddress(),
                FUNC_NAME_QUERY_SDP_MESSAGE_SEQ,
                inputParameters,
                outputParameters
        );

        if (ObjectUtil.isEmpty(result)) {
            throw new RuntimeException(
                    String.format(
                            "empty output for querySDPMessageSeq by key ( %s, %s, %s, %s )",
                            senderDomain, fromAddress, receiverDomain, toAddress
                    )
            );
        }
        if (!StrUtil.equals(result.get(0).getTypeAsString(), Uint32.DEFAULT.getTypeAsString())) {
            throw new RuntimeException(
                    String.format(
                            "wrong type for querySDPMessageSeq's result for key( %s, %s, %s, %s )",
                            senderDomain, fromAddress, receiverDomain, toAddress
                    )
            );
        }

        return ((Uint32) result.get(0)).getValue().longValue();
    }

    @Override
    public void setProtocol(String protocolAddress, String protocolType) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())) {
            throw new RuntimeException("empty am contract in bbc context");
        }

        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Address(protocolAddress));
        inputParameters.add(new Uint32(Long.parseLong(protocolType)));

        TransactionReceipt receipt = this.citaCloudClient.callContract(
                this.bbcContext.getAuthMessageContract().getContractAddress(),
                FUNC_NAME_SET_PROTOCOL,
                inputParameters,
                true
        );

        System.out.printf("setProtocol(%s, %s) by tx %s\n", protocolAddress, protocolType, receipt.getTransactionHash());

        if (checkIfAMReady()) {
            this.bbcContext.getAuthMessageContract().setStatus(ContractStatusEnum.CONTRACT_READY);
        }
    }

    @Override
    public CrossChainMessageReceipt relayAuthMessage(byte[] rawMessage) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())) {
            throw new RuntimeException("empty am contract in bbc context");
        }

        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new DynamicBytes(rawMessage));

        TransactionReceipt receipt = this.citaCloudClient.callContract(
                this.bbcContext.getAuthMessageContract().getContractAddress(),
                FUNC_NAME_RECV_PKG_FROM_RELAYER,
                inputParameters,
                false
        );

        CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();
        crossChainMessageReceipt.setSuccessful(citaCloudClient.isTxExecuteSuccess(receipt));
        crossChainMessageReceipt.setConfirmed(
                ObjectUtil.isNotNull(receipt.getBlockNumber()) && receipt.getBlockNumber().longValue() > 0
        );
        crossChainMessageReceipt.setTxhash(receipt.getTransactionHash());
        crossChainMessageReceipt.setErrorMsg(StrUtil.emptyToDefault(receipt.getErrorMessage(), ""));

        return crossChainMessageReceipt;
    }

    @Override
    public void setupAuthMessageContract() {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNotNull(this.bbcContext.getAuthMessageContract())
                && StrUtil.isNotEmpty(this.bbcContext.getAuthMessageContract().getContractAddress())) {
            // If the contract has been pre-deployed and the contract address is configured in the configuration file,
            // there is no need to redeploy.
            return;
        }

        String rawContractBin = FileUtil.readString(
                new ClassPathResource("contract/v0.1/am.bin", CITACloudBBCService.class.getClassLoader()).getUrl(),
                StandardCharsets.UTF_8
        );
        if (ObjectUtil.isNull(rawContractBin)) {
            throw new RuntimeException("empty AM contract binary with version " + this.config.getSysContractVersion());
        }

        String amAddress = this.citaCloudClient.deployContractWithoutConstructor(rawContractBin, true);
        if (StrUtil.isEmpty(amAddress)) {
            throw new RuntimeException("unexpected contract address");
        }

        AuthMessageContract authMessageContract = new AuthMessageContract();
        authMessageContract.setContractAddress(amAddress);
        authMessageContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
        bbcContext.setAuthMessageContract(authMessageContract);

        System.out.printf("setup AM contract successful: %s\n", amAddress);
    }

    @Override
    public void setupSDPMessageContract() {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNotNull(this.bbcContext.getSdpContract())
                && StrUtil.isNotEmpty(this.bbcContext.getSdpContract().getContractAddress())) {
            // If the contract has been pre-deployed and the contract address is configured in the configuration file,
            // there is no need to redeploy.
            return;
        }

        String rawContractBin = FileUtil.readString(
                new ClassPathResource("contract/v0.1/sdp.bin", CITACloudBBCService.class.getClassLoader()).getUrl(),
                StandardCharsets.UTF_8
        );
        if (StrUtil.isEmpty(rawContractBin)) {
            throw new RuntimeException("empty SDP contract binary for version " + this.config.getSysContractVersion());
        }

        String sdpAddress = this.citaCloudClient.deployContractWithoutConstructor(rawContractBin, true);
        if (StrUtil.isEmpty(sdpAddress)) {
            throw new RuntimeException("unexpected contract address");
        }

        SDPContract sdpContract = new SDPContract();
        sdpContract.setContractAddress(sdpAddress);
        sdpContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
        bbcContext.setSdpContract(sdpContract);

        System.out.printf("setup SDP contract successful: %s\n", sdpAddress);
    }

    @Override
    public void setAmContract(String contractAddress) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getSdpContract())) {
            throw new RuntimeException("empty sdp contract in bbc context");
        }

        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Address(contractAddress));

        TransactionReceipt receipt = this.citaCloudClient.callContract(
                this.bbcContext.getSdpContract().getContractAddress(),
                FUNC_NAME_SET_AM_CONTRACT,
                inputParameters,
                true
        );

        System.out.printf("setAmContract(%s) by tx %s\n", contractAddress, receipt.getTransactionHash());

        if (checkIfSDPReady()) {
            this.bbcContext.getSdpContract().setStatus(ContractStatusEnum.CONTRACT_READY);
        }
    }

    @Override
    public void setLocalDomain(String domain) {
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getSdpContract())) {
            throw new RuntimeException("empty sdp contract in bbc context");
        }

        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Utf8String(domain));

        TransactionReceipt receipt = this.citaCloudClient.callContract(
                this.bbcContext.getSdpContract().getContractAddress(),
                FUNC_NAME_SET_LOCAL_DOMAIN,
                inputParameters,
                true
        );

        System.out.printf("setLocalDomain(%s) by tx %s\n", domain, receipt.getTransactionHash());

        if (checkIfSDPReady()) {
            this.bbcContext.getSdpContract().setStatus(ContractStatusEnum.CONTRACT_READY);
        }
    }

    public CITACloudAutoRefreshClient getCitaCloudClient() {
        return citaCloudClient;
    }

    public CITACloudConfig getConfig() {
        return config;
    }

    private boolean checkIfSDPReady() {
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(TypeReference.create(Bytes32.class));

        List<Type> result = this.citaCloudClient.localCallContract(
                this.bbcContext.getSdpContract().getContractAddress(),
                FUNC_NAME_GET_LOCAL_DOMAIN,
                new ArrayList<>(),
                outputParameters
        );
        byte[] domainHash = ((Bytes32) result.get(0)).getValue();
        if (ObjectUtil.isEmpty(domainHash)) {
            return false;
        }

        outputParameters = new ArrayList<>();
        outputParameters.add(TypeReference.create(Address.class));

        result = this.citaCloudClient.localCallContract(
                this.bbcContext.getSdpContract().getContractAddress(),
                FUNC_NAME_GET_AM_ADDRESS,
                new ArrayList<>(),
                outputParameters
        );

        String amAddress = ((Address) result.get(0)).getValue();
        return !StrUtil.isEmpty(amAddress);
    }

    private boolean checkIfAMReady() {
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(Uint32.DEFAULT);

        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(TypeReference.create(Address.class));

        List<Type> result = this.citaCloudClient.localCallContract(
                this.bbcContext.getAuthMessageContract().getContractAddress(),
                FUNC_NAME_GET_PROTOCOL,
                inputParameters,
                outputParameters
        );

        return StrUtil.equals(
                ((Address) result.get(0)).getValue(),
                this.bbcContext.getSdpContract().getContractAddress()
        );
    }
}
