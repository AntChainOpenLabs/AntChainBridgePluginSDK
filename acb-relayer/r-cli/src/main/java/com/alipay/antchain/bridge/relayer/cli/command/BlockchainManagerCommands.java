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

package com.alipay.antchain.bridge.relayer.cli.command;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.annotation.Resource;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.bta.BlockchainTrustAnchorFactory;
import com.alipay.antchain.bridge.commons.core.bta.IBlockchainTrustAnchor;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import com.alipay.antchain.bridge.relayer.cli.glclient.GrpcClient;
import com.alipay.antchain.bridge.relayer.facade.admin.types.BlockchainId;
import com.alipay.antchain.bridge.relayer.facade.admin.types.ConsensusStateInfo;
import com.alipay.antchain.bridge.relayer.facade.admin.types.SysContractsInfo;
import lombok.Getter;
import org.springframework.shell.standard.*;

@Getter
@ShellCommandGroup(value = "Commands about Blockchain")
@ShellComponent
public class BlockchainManagerCommands extends BaseCommands {

    @Resource
    private GrpcClient grpcClient;

    @Resource
    private PtcCommands ptcCommands;

    @Override
    public String name() {
        return "blockchain";
    }

    @ShellMethod(value = "Get the local blockchain ID for specified domain")
    Object getBlockchainIdByDomain(@ShellOption(help = "the blockchain domain name") String domain) {
        return queryAPI("getBlockchainIdByDomain", domain);
    }

    @ShellMethod(value = "Get the local blockchain data for specified domain")
    Object getBlockchain(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId
    ) {
        return queryAPI("getBlockchain", product, blockchainId);
    }

    @ShellMethod(value = "Get the local blockchain BBC contracts information")
    Object getBlockchainContracts(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId
    ) {
        return queryAPI("getBlockchainContracts", product, blockchainId);
    }

    @ShellMethod(value = "Get the local blockchain heights where anchor service runs on")
    Object getBlockchainHeights(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId
    ) {
        return queryAPI("getBlockchainHeights", product, blockchainId);
    }

    @ShellMethod(value = "Add a specified blockchain configuration to start the anchor service")
    Object addBlockchainAnchor(
            @ShellOption(help = "Blockchain BBC service plugin product name, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId,
            @ShellOption(help = "Domain for blockchain") String domain,
            @ShellOption(help = "Plugin server ID the blockchain plugin running on") String pluginServerId,
            @ShellOption(help = "Alias for blockchain", defaultValue = "") String alias,
            @ShellOption(help = "Description for blockchain", defaultValue = "") String desc,
            @ShellOption(help = "If BBC plugin support reliable crosschain relay, true or false", defaultValue = "false") String ifReliable,
            @ShellOption(valueProvider = FileValueProvider.class, help = "The file path to configuration file required by the `BBCService` in blockchain plugin") String confFile
    ) {
        return queryAPI(
                "addBlockchainAnchor",
                product, blockchainId, domain, pluginServerId, alias, desc, confFile, ifReliable
        );
    }

    @ShellMethod(value = "Send request to ask BBC service to deploy BBC contracts")
    Object setupBBCContracts(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId
    ) {
        return queryAPI("setupBBCContracts", product, blockchainId);
    }

    @ShellMethod(value = "Help to build the BTA")
    Object buildBTAv1(
            @ShellOption(help = "Blockchain domain") String blockchainDomain,
            @ShellOption(help = "Blockchain BBC service plugin product name, e.g. mychain010") String subjectProduct,
            @ShellOption(help = "BTA subject version default query from relayer", defaultValue = "") String subjectVersion,
            @ShellOption(
                    help = "The blockchain subject identity base64 encoded, default check the menu of the hetero-blockchain bbc plugin"
            ) String subjectIdentityBase64,
            @ShellOption(
                    help = "The block height number where crosschain service started, default query the latest height from relayer",
                    defaultValue = ""
            ) String initHeight,
            @ShellOption(
                    help = "The block hash where crosschain service started, default query the latest blockhash from relayer",
                    defaultValue = ""
            ) String initBlockHash,
            @ShellOption(
                    help = "Auth Message contract address, default query it from relayer", defaultValue = ""
            ) String authMessageContractAddress,
            @ShellOption(
                    help = "Extension information for BTA, usually used for PTC verification"
            ) String extensionBase64,
            @ShellOption(
                    help = "Ptc service id to verify the BTA and issue the TpBTA", defaultValue = ""
            ) String ptcServiceId,
            @ShellOption(
                    help = "Ptc's PEM crosschain certificate", defaultValue = ""
            ) String ptcCertPem,
            @ShellOption(
                    help = "The algorithm for domain certificate owner going to sign the BTA, default KECCAK256_WITH_SECP256K1",
                    defaultValue = "KECCAK256_WITH_SECP256K1",
                    valueProvider = EnumValueProvider.class
            ) SignAlgoEnum signAlgo,
            @ShellOption(
                    help = "The private key of domain certificate owner", valueProvider = FileValueProvider.class
            ) String domainOwnerPrivateKeyFile,
            @ShellOption(
                    help = "The public key of domain certificate owner", valueProvider = FileValueProvider.class
            ) String domainOwnerPublicKeyFile
    ) {

        try {
            BigInteger initHeightNum;
            byte[] initBlockHashRaw;
            if (StrUtil.isEmpty(initHeight)) {
                String res = (String) queryLatestConsensusStateInfo(blockchainDomain);
                if (!JSONUtil.isTypeJSON(res)) {
                    return "failed to get latest consensus state info: " + res;
                }
                ConsensusStateInfo consensusStateInfo = JSON.parseObject(res, ConsensusStateInfo.class);
                initHeightNum = new BigInteger(consensusStateInfo.getHeight());
                initBlockHashRaw = HexUtil.decodeHex(consensusStateInfo.getHash());
            } else {
                initHeightNum = new BigInteger(initHeight);
                initBlockHashRaw = HexUtil.decodeHex(initBlockHash);
            }

            int subjectVersionNum = StrUtil.isEmpty(subjectVersion) ?
                    Integer.parseInt((String) getLatestVersionOfBta(blockchainDomain)) + 1 :
                    Integer.parseInt(subjectVersion);

            BlockchainId blockchainId = JSON.parseObject((String) getBlockchainIdByDomain(blockchainDomain), BlockchainId.class);
            if (!StrUtil.equals(blockchainId.getProduct(), subjectProduct)) {
                return "subject product not match";
            }

            byte[] amId;
            if (StrUtil.isEmpty(authMessageContractAddress)) {
                amId = getAmId(
                        JSON.parseObject(
                                (String) getBlockchainContracts(subjectProduct, blockchainId.getBlockchainId()),
                                SysContractsInfo.class
                        ).getAmContract()
                );
            } else {
                amId = getAmId(authMessageContractAddress);
            }

            if (StrUtil.isAllEmpty(ptcCertPem, ptcServiceId)) {
                return "please input ptc service id or ptc certificate pem";
            }

            ObjectIdentity ptcOid = CrossChainCertificateUtil.readCrossChainCertificateFromPem(
                    StrUtil.isNotEmpty(ptcServiceId) ? ((String) ptcCommands.getPtcCert(ptcServiceId)).getBytes() : ptcCertPem.getBytes()
            ).getCredentialSubjectInstance().getApplicant();

            Path privateKeyPath = Paths.get(domainOwnerPrivateKeyFile);
            if (!Files.exists(privateKeyPath)) {
                return "domain owner private key file not exists";
            }
            PrivateKey privateKey = signAlgo.getSigner().readPemPrivateKey(Files.readAllBytes(privateKeyPath));

            Path publicKeyPath = Paths.get(domainOwnerPublicKeyFile);
            if (!Files.exists(publicKeyPath)) {
                return "domain owner public key file not exists";
            }

            PublicKey publicKey = readPublicKeyFromPem(Files.readAllBytes(publicKeyPath));

            IBlockchainTrustAnchor bta = BlockchainTrustAnchorFactory.createBTAv1(
                    blockchainDomain,
                    subjectProduct,
                    subjectVersionNum,
                    publicKey.getEncoded(),
                    signAlgo,
                    Base64.decode(subjectIdentityBase64),
                    initHeightNum,
                    initBlockHashRaw,
                    amId,
                    ptcOid,
                    Base64.decode(extensionBase64)
            );

            bta.sign(privateKey);

            return "your BTA is: \n" + Base64.encode(bta.encode());
        } catch (Throwable t) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", t);
        }
    }

    @ShellMethod(value = "Query the latest subject version for existed BTA")
    Object getLatestVersionOfBta(
            @ShellOption(help = "Blockchain domain to query") String blockchainDomain
    ) {
        return queryAPI("getLatestVersionOfBta", blockchainDomain);
    }

    @ShellMethod(value = "Register the BTA of blockchain and relayer would ask PTC to issue the TpBTA")
    Object registerTpBta(
            @ShellOption(help = "Ptc service id to verify and issue the TpBTA") String ptcServiceId,
            @ShellOption(help = "The serialized base64 encoded BTA") String rawBase64Bta
    ) {
        return queryAPI("registerTpBta", ptcServiceId, rawBase64Bta);
    }

    @ShellMethod(value = "Query the valid TpBTA list")
    Object queryValidTpBtaList(
            @ShellOption(help = "The blockchain domain as sender") String domain
    ) {
        return queryAPI("queryValidTpBtaList", domain);
    }

    @ShellMethod(value = "Query the exact TpBTA by crosschain lane and TpBTA version number")
    Object queryExactTpBta(
            @ShellOption(help = "Sender blockchain domain") String senderDomain,
            @ShellOption(help = "Receiver blockchain domain", defaultValue = "") String receiverDomain,
            @ShellOption(help = "Sender Identity", defaultValue = "") String senderIdentity,
            @ShellOption(help = "Receiver Identity", defaultValue = "") String receiverIdentity,
            @ShellOption(help = "TpBTA version number, default the latest", defaultValue = "-1") String tpBtaVersion
    ) {
        CrossChainLane tpbtaLane;
        if (!senderIdentity.isEmpty()) {
            tpbtaLane = new CrossChainLane(
                    new CrossChainDomain(senderDomain),
                    new CrossChainDomain(receiverDomain),
                    getIdentity(senderIdentity),
                    getIdentity(receiverIdentity)
            );
        } else if (!receiverDomain.isEmpty()) {
            tpbtaLane = new CrossChainLane(new CrossChainDomain(senderDomain), new CrossChainDomain(receiverDomain));
        } else {
            tpbtaLane = new CrossChainLane(new CrossChainDomain(senderDomain));
        }

        return queryAPI("queryExactTpBta", tpbtaLane.getLaneKey(), tpBtaVersion);
    }

    @ShellMethod(value = "Mark the blockchain to deploy BBC contracts async")
    Object deployBBCContractsAsync(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId
    ) {
        return queryAPI("deployBBCContractsAsync", product, blockchainId);
    }

    @ShellMethod(value = "Update the whole blockchain anchor configuration")
    Object updateBlockchainAnchor(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId,
            @ShellOption(help = "Alias for blockchain", defaultValue = "") String alias,
            @ShellOption(help = "Description for blockchain", defaultValue = "") String desc,
            @ShellOption(help = "The configuration value in JSON string") String clientConfig
    ) {
        return queryAPI("updateBlockchainAnchor", product, blockchainId, alias, desc, clientConfig);
    }

    @ShellMethod(value = "Update the specified key-value in blockchain anchor configuration")
    Object updateBlockchainProperty(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId,
            @ShellOption(help = "configuration key") String confKey,
            @ShellOption(help = "configuration value") String confValue
    ) {
        return queryAPI("updateBlockchainProperty", product, blockchainId, confKey, confValue);
    }

    @ShellMethod(value = "Start the blockchain anchor service in Relayer")
    Object startBlockchainAnchor(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId
    ) {
        return queryAPI("startBlockchainAnchor", product, blockchainId);
    }

    @ShellMethod(value = "Stop the blockchain anchor service in Relayer")
    Object stopBlockchainAnchor(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId
    ) {

        return queryAPI("stopBlockchainAnchor", product, blockchainId);
    }

    @ShellMethod(value = "Set max limit for the transaction pending but not committed on blockchain receiving cross-chain messages")
    Object setTxPendingLimit(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId,
            @ShellOption(help = "The limit value for transaction pending") Integer txPendingLimit
    ) {
        return queryAPI("setTxPendingLimit", product, blockchainId, Integer.toString(txPendingLimit));
    }

    @ShellMethod(value = "Query SDP message sequence number on the specified direction")
    Object querySDPMsgSeq(
            @ShellOption(help = "Product type for blockchain receiving cross-chain messages") String receiverProduct,
            @ShellOption(help = "Local blockchain ID for blockchain receiving cross-chain messages") String receiverBlockchainId,
            @ShellOption(help = "Blockchain domain for blockchain sending cross-chain messages") String senderDomain,
            @ShellOption(help = "The sender contract identity, e.g. 0x1f9840a85d5aF5bf1D1762F925BDADdC4201F984") String sender,
            @ShellOption(help = "The receiver contract identity, e.g. 0x1f9840a85d5aF5bf1D1762F925BDADdC4201F984") String receiver
    ) {
        return queryAPI("querySDPMsgSeq", receiverProduct, receiverBlockchainId, senderDomain, sender, receiver);
    }

    @ShellMethod(value = "Query the latest consensus-state(block) info for the specified blockchain")
    Object queryLatestConsensusStateInfo(
            @ShellOption(help = "Blockchain domain") String blockchainDomain
    ) {
        return queryAPI("readLatestConsensusInfo", blockchainDomain);
    }

    @ShellMethod(value = "")
    Object retrySdpMessage(
            @ShellOption(help = "Auth msg id pointing to this SDP message, from DB table 'sdp_msg_pool' field 'auth_msg_id'") Long authMsgId
    ) {
        return queryAPI("retrySdpMessage", Long.toString(authMsgId));
    }
}
