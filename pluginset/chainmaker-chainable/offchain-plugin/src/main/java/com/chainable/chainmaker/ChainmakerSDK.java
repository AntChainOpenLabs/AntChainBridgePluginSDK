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

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.chainmaker.pb.common.ChainmakerBlock.BlockInfo;
import org.chainmaker.pb.common.ChainmakerTransaction.TransactionInfo;
import org.chainmaker.pb.common.ResultOuterClass;
import org.chainmaker.sdk.*;
import org.chainmaker.sdk.config.NodeConfig;
import org.chainmaker.sdk.config.SdkConfig;
import org.chainmaker.sdk.crypto.ChainMakerCryptoSuiteException;
import org.chainmaker.sdk.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint32;
import org.yaml.snakeyaml.Yaml;

import cn.hutool.core.util.HexUtil;


public class ChainmakerSDK {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChainmakerSDK.class);
    public static final long rpcCallTimeout = 10000;
    public static final long syncResultTimeout = 10000;

    static final String USER1_KEY_PATH = "src/main/resources/crypto-config/org_id/user1/user1.sign.key";
    static final String USER1_CERT_PATH = "src/main/resources/crypto-config/org_id/user1/user1.sign.crt";

    static String USER1_TLS_KEY_PATH = "src/main/resources/crypto-config/org_id/user1/user1.tls.key";
    static String USER1_TLS_CERT_PATH = "src/main/resources/crypto-config/org_id/user1/user1.tls.crt";

    static final String ORG_ID1 = "org_id";
    static final String CONTRACT_ARGS_EVM_PARAM = "data";

    static String SDK_CONFIG = "src/main/resources/sdk_config.yml";
    static String PLUGIN_CONFIG = "src/main/resources/plugin_config.yml";

    public static ChainClient chainClient;
    public static PluginConfig pluginConfig;
    static ChainManager chainManager;
    static User user;

    public ChainmakerSDK() {
        LOGGER.info("init sdk instance");
    }

    public void initSDK(byte[] conf) throws Exception {
        LOGGER.info("sdk init with: " + new String(conf));
        inItChainClient();
        initSystemContractAddress();
        LOGGER.info("sdk init success");
    }

    public void shutdown() {
        LOGGER.info("exit sdk");
        chainClient.stop();
    }

    public static void inItChainClient() throws Exception {
        Yaml yaml = new Yaml();
        Path path = Paths.get(SDK_CONFIG);
        InputStream in = Files.newInputStream(path);

        SdkConfig sdkConfig;
        sdkConfig = yaml.loadAs(in, SdkConfig.class);
        assert in != null;
        in.close();

        LOGGER.info("[init chainclient] parse yaml file success");

        for (NodeConfig nodeConfig : sdkConfig.getChainClient().getNodes()) {
            List<byte[]> tlsCaCertList = new ArrayList<>();
            if (nodeConfig.getTrustRootPaths() != null) {
                for (String rootPath : nodeConfig.getTrustRootPaths()) {
                    List<String> filePathList = FileUtils.getFilesByPath(rootPath);
                    for (String filePath : filePathList) {
                        tlsCaCertList.add(FileUtils.getFileBytes(filePath));
                    }
                }
            }
            byte[][] tlsCaCerts = new byte[tlsCaCertList.size()][];
            tlsCaCertList.toArray(tlsCaCerts);
            nodeConfig.setTrustRootBytes(tlsCaCerts);
        }

        chainManager = ChainManager.getInstance();
        chainClient = chainManager.getChainClient(sdkConfig.getChainClient().getChainId());

        if (chainClient == null) {
            chainClient = chainManager.createChainClient(sdkConfig);
        }

        user = new User(ORG_ID1,
                FileUtils.getResourceFileBytes(USER1_KEY_PATH),
                FileUtils.getResourceFileBytes(USER1_CERT_PATH),
                FileUtils.getResourceFileBytes(USER1_TLS_KEY_PATH),
                FileUtils.getResourceFileBytes(USER1_TLS_CERT_PATH), false);

        LOGGER.info("[init chainclient] user init success");
    }

    public static void initSystemContractAddress() throws Exception {
        Yaml yaml = new Yaml();
        Path path = Paths.get(PLUGIN_CONFIG);
        InputStream in = Files.newInputStream(path);

        pluginConfig = yaml.loadAs(in, PluginConfig.class);
        assert in != null;
        in.close();

        LOGGER.info("[init contract address] parse yaml file success");
    }

    public BlockInfo queryABlock(Long height) {
        try {
            BlockInfo blockInfo = chainClient.getBlockByHeight(height, false, rpcCallTimeout);
            return blockInfo;
        } catch (ChainMakerCryptoSuiteException | ChainClientException e) {
            LOGGER.info("get block by height error:"+e);
            throw new RuntimeException(e);
        }
    }

    public TransactionInfo queryTx(String txId) {
        try {
            return chainClient.getTxByTxId(txId, rpcCallTimeout);
        } catch (ChainMakerCryptoSuiteException | ChainClientException e) {
            LOGGER.info("get tx info by tx id error: "+e);
            throw new RuntimeException(e);
        }
    }

    public Long queryLatestHeight()  {
        try {
          return chainClient.getCurrentBlockHeight(rpcCallTimeout);
        } catch (ChainMakerCryptoSuiteException | ChainClientException e) {
            LOGGER.info("get current block height error: "+e);
            throw new RuntimeException(e);
        }
    }

    public void setProtocol(String contractAddress, String funcName, String protocolAddress, String protocolType) {;
        invokeEVMFunction(contractAddress, new Function(funcName,
                Arrays.asList(new Address(protocolAddress), new Uint32(new BigInteger(protocolType))),
                Collections.emptyList()));
    }

    public void setAmContract(String contractAddress, String funcName, String amContractAddress) {
        invokeEVMFunction(contractAddress, new Function(funcName,
                Arrays.asList(new Address(amContractAddress)),
                Collections.emptyList()));
    }

    public void setLocalDomain(String contractAddress, String funcName, String domain) {
        invokeEVMFunction(contractAddress, new Function(funcName,
                Arrays.asList(new Utf8String(domain)),
                Collections.emptyList()));
    }

    public ResultOuterClass.TxResponse querySDPMessageSeq(String contractAddress, String funcName, String senderDomain, String fromAddress, String receiverDomain, String toAddress) {
        return invokeEVMFunction(contractAddress, new Function(funcName,
                Arrays.asList(new Utf8String(senderDomain),
                        new Bytes32(HexUtil.decodeHex(fromAddress)),
                        new Utf8String(receiverDomain),
                        new Bytes32(HexUtil.decodeHex(toAddress))
                ),
                Collections.emptyList())
        );
    }

    public ResultOuterClass.TxResponse relayAuthMessage(String contractAddress, String funcName, byte[] rawBytes) {
        return invokeEVMFunction(contractAddress, new Function(funcName,
                Arrays.asList(new DynamicBytes(rawBytes)),
                Collections.emptyList()));
    }

    private ResultOuterClass.TxResponse invokeEVMFunction(String contractAddress, Function function) {
        Map<String, byte[]> params = new HashMap<>();
        String methodDataStr = FunctionEncoder.encode(function);
        String method = methodDataStr.substring(0, 10);
        params.put(CONTRACT_ARGS_EVM_PARAM, methodDataStr.getBytes());

        ResultOuterClass.TxResponse response = null;
        try {
            LOGGER.info("[Invoke] contract address: "+contractAddress+", method: "+method+", method data string "+methodDataStr);
            response = chainClient.invokeContract(contractAddress,
                    method, null, params, rpcCallTimeout, syncResultTimeout);
        } catch (SdkException e) {
            e.printStackTrace();
        }

        return response;
    }

    public class PluginConfig {
        String AMContractAddress;
        String SDPContractAddress;

        public String getAMContractAddress() {
            return AMContractAddress;
        }

        public void setAMContractAddress(String AMContractAddress) {
            this.AMContractAddress = AMContractAddress;
        }

        public String getSDPContractAddress() {
            return SDPContractAddress;
        }

        public void setSDPContractAddress(String SDPContractAddress) {
            this.SDPContractAddress = SDPContractAddress;
        }
    }

    public String getAmContractAddress() {
        return this.pluginConfig.getAMContractAddress();
    }

    public String getSdpContractAddress() {
        return this.pluginConfig.getSDPContractAddress();
    }
}
