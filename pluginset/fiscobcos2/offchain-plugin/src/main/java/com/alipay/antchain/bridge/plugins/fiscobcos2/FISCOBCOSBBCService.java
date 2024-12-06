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
package com.alipay.antchain.bridge.plugins.fiscobcos2;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.plugins.fiscobcos2.abi.AuthMsg;
import com.alipay.antchain.bridge.plugins.fiscobcos2.abi.SDPMsg;
import com.alipay.antchain.bridge.plugins.lib.BBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import com.webank.wedpr.crypto.NativeInterface;
import lombok.Getter;
import lombok.SneakyThrows;
import org.fisco.bcos.sdk.BcosSDK;
import org.fisco.bcos.sdk.abi.ABICodecException;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.client.protocol.response.BcosBlock;
import org.fisco.bcos.sdk.client.protocol.response.BlockNumber;
import org.fisco.bcos.sdk.config.ConfigOption;
import org.fisco.bcos.sdk.config.model.*;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.crypto.keypair.SM2KeyPair;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.fisco.bcos.sdk.transaction.codec.decode.TransactionDecoderInterface;
import org.fisco.bcos.sdk.transaction.codec.decode.TransactionDecoderService;
import org.fisco.bcos.sdk.transaction.manager.AssembleTransactionProcessor;
import org.fisco.bcos.sdk.transaction.manager.TransactionProcessorFactory;
import org.fisco.bcos.sdk.transaction.model.dto.TransactionResponse;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.alipay.antchain.bridge.plugins.fiscobcos2.abi.AuthMsg.SENDAUTHMESSAGE_EVENT;

@BBCService(products = "fiscobcos2", pluginId = "plugin-fiscobcos2")
@Getter
public class FISCOBCOSBBCService extends AbstractBBCService {

    static private String TX_RECEIPT_STATUS_SUCCESS = "0x0";

    private FISCOBCOSConfig config;

    private BcosSDK sdk;
    private Client client;
    private CryptoKeyPair keyPair;
    private AssembleTransactionProcessor transactionProcessorAM;
    private AssembleTransactionProcessor transactionProcessorSDP;

    private AbstractBBCContext bbcContext;

    @Override
    @SneakyThrows
    public void startup(AbstractBBCContext abstractBBCContext) {
        getBBCLogger().info("BSN~FISCO-BCOS BBCService startup with context: {}", new String(abstractBBCContext.getConfForBlockchainClient()));

        // init NativeInterface
        Future<?> future = ThreadUtil.execAsync(() -> {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            NativeInterface.secp256k1GenKeyPair();
        });
        future.get();

        if (ObjectUtil.isNull(abstractBBCContext)) {
            throw new RuntimeException("null bbc context");
        }
        if (ObjectUtil.isEmpty(abstractBBCContext.getConfForBlockchainClient())) {
            throw new RuntimeException("empty blockchain client conf");
        }

        // 1. obtain the configuration information
        try {
            config = FISCOBCOSConfig.fromJsonString(new String(abstractBBCContext.getConfForBlockchainClient()));
            getBBCLogger().info("BSN~ startup with config: {}", JSON.toJSONString(config));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (StrUtil.isEmpty(config.getConnectPeer())) {
            throw new RuntimeException("Address of peer to connect is empty");
        }

        if (StrUtil.isEmpty(config.getGroupID())) {
            throw new RuntimeException("groupID to which the connected node belongs is empty");
        }

        // 2. connect to the FISCO-BCOS network
        try {
            ConfigProperty configProperty = new ConfigProperty();

            // 实例化 cryptoMaterial
            Map<String, Object> cryptoMaterial = new HashMap<>();
            cryptoMaterial.put("certPath", "/Users/liyuan/Documents/business/自然资源部/20241129/sdk/gm");// certPath: 证书路径
            cryptoMaterial.put("caCert", "/Users/liyuan/Documents/business/自然资源部/20241129/sdk/gm/gmca.crt");// caCert: CA证书路径
            cryptoMaterial.put("sslCert", "/Users/liyuan/Documents/business/自然资源部/20241129/sdk/gm/gmsdk.crt");// sslCert: SDK证书
            cryptoMaterial.put("sslKey", "/Users/liyuan/Documents/business/自然资源部/20241129/sdk/gm/gmsdk.key");// sslKey: SDK私钥
            cryptoMaterial.put("enSslCert", "/Users/liyuan/Documents/business/自然资源部/20241129/sdk/gm/gmensdk.crt");// enSslCert: 国密SSL的SDK证书
            cryptoMaterial.put("enSslKey", "/Users/liyuan/Documents/business/自然资源部/20241129/sdk/gm/gmensdk.key");// enSslKey: 国密SSL的SDK私钥
            cryptoMaterial.put("sslCryptoType", "1");// 0非国密  1 国密
            cryptoMaterial.put("useSMCrypto", "true");
            cryptoMaterial.put("disableSsl", "false");
            configProperty.cryptoMaterial = cryptoMaterial;

            // 实例化 network
            Map<String, Object> network = new HashMap<>();
            network.put("messageTimeout", config.getMessageTimeout());
            network.put("peers", new ArrayList<>(Collections.singletonList(config.getConnectPeer())));
            configProperty.network = network;

            // 实例化 account
            Map<String, Object> account = new HashMap<>();
            account.put("keyStoreDir", config.getKeyStoreDir());
            account.put("accountFileFormat", config.getAccountFileFormat());
            configProperty.account = account;

            // 实例化 threadPool
            configProperty.threadPool = new HashMap<>();

            // 实例化 amop
            configProperty.amop = new ArrayList<>();

            ConfigOption configOption = new ConfigOption(configProperty);

            CryptoMaterialConfig cryptoMaterialConfig = new CryptoMaterialConfig(configProperty,1);
            cryptoMaterialConfig.setCaCertPath("/Users/liyuan/Documents/business/自然资源部/20241129/sdk/gm/gmca.crt");
            cryptoMaterialConfig.setSdkCertPath("/Users/liyuan/Documents/business/自然资源部/20241129/sdk/gm/gmsdk.crt");
            cryptoMaterialConfig.setSdkPrivateKeyPath("/Users/liyuan/Documents/business/自然资源部/20241129/sdk/gm/gmsdk.key");

            configOption.setCryptoMaterialConfig(cryptoMaterialConfig);

            configOption.setAccountConfig(new AccountConfig(configProperty));
            configOption.setAmopConfig(new AmopConfig(configProperty));
            configOption.setNetworkConfig(new NetworkConfig(configProperty));
            configOption.setThreadPoolConfig(new ThreadPoolConfig(configProperty));

            getBBCLogger().info("BSN~FISCO-BCOS connect peer : {} start", config.getConnectPeer());
            // Initialize BcosSDK
            sdk = new BcosSDK(configOption);
            getBBCLogger().info("BSN~FISCO-BCOS connect peer : {} end", config.getConnectPeer());
            getBBCLogger().info("BSN~FISCO-BCOS get client : {} start", config.getGroupID());
            // Initialize the client for the group
            client = sdk.getClient(Integer.valueOf(config.getGroupID()));
            getBBCLogger().info("BSN~FISCO-BCOS get client : {} end", config.getGroupID());

        } catch (Exception e) {
            getBBCLogger().info(e.toString());
            throw new RuntimeException(String.format("failed to connect fisco-bcos to peer:%s, group:%s", config.getConnectPeer(), config.getGroupID()), e);
        }

        // 3. initialize keypair and create transaction processor
        this.keyPair = client.getCryptoSuite().getCryptoKeyPair();

        //直接加载非国密私钥
        /*ECDSAKeyPair keyFacotry = new ECDSAKeyPair();
        this.keyPair = keyFacotry.createKeyPair("93f3b7bb27feab401ee081cda6b83ad0a78403a58871fafbadfe531c1af2a56a");
*/
        //直接加载国密私钥
        // 创建国密类型的KeyFactory
        SM2KeyPair keyFacotry = new SM2KeyPair();
        // 从十六进制字符串加载hexPrivateKey
        this.keyPair= keyFacotry.createKeyPair("93f3b7bb27feab401ee081cda6b83ad0a78403a58871fafbadfe531c1af2a56a");
        client.getCryptoSuite().setCryptoKeyPair(this.keyPair);

        this.transactionProcessorAM = TransactionProcessorFactory.createAssembleTransactionProcessor(
                client,
                this.keyPair,
                "AuthMsg",
                AuthMsg.ABI_ARRAY[0],
                AuthMsg.SM_BINARY_ARRAY[0]
        );
        this.transactionProcessorSDP = TransactionProcessorFactory.createAssembleTransactionProcessor(
                client,
                this.keyPair,
                "SDPMsg",
                SDPMsg.ABI_ARRAY[0],
                SDPMsg.SM_BINARY_ARRAY[0]
        );

        // 4. set context
        this.bbcContext = abstractBBCContext;

        // 5. set the pre-deployed contracts into context
        if (ObjectUtil.isNull(abstractBBCContext.getAuthMessageContract())
                && StrUtil.isNotEmpty(this.config.getAmContractAddressDeployed())) {
            AuthMessageContract authMessageContract = new AuthMessageContract();
            authMessageContract.setContractAddress(this.config.getAmContractAddressDeployed());
            authMessageContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
            this.bbcContext.setAuthMessageContract(authMessageContract);
        }

        if (ObjectUtil.isNull(abstractBBCContext.getSdpContract())
                && StrUtil.isNotEmpty(this.config.getSdpContractAddressDeployed())) {
            SDPContract sdpContract = new SDPContract();
            sdpContract.setContractAddress(this.config.getSdpContractAddressDeployed());
            sdpContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
            this.bbcContext.setSdpContract(sdpContract);
        }

        getBBCLogger().info("FISCO-BCOS BBCService startup success for {}", this.config.getConnectPeer());
    }

    @Override
    public void shutdown() {
        getBBCLogger().info("BSN~ shutdown FISCO-BCOS BBCService!");
        this.client.stop();
    }

    @Override
    public AbstractBBCContext getContext() {
        getBBCLogger().info("BSN~ getContext FISCO-BCOS BBCService!");
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }

        getBBCLogger().debug("FISCO-BCOS BBCService context (amAddr: {}, amStatus: {}, sdpAddr: {}, sdpStatus: {})",
                this.bbcContext.getAuthMessageContract() != null ? this.bbcContext.getAuthMessageContract().getContractAddress() : "",
                this.bbcContext.getAuthMessageContract() != null ? this.bbcContext.getAuthMessageContract().getStatus() : "",
                this.bbcContext.getSdpContract() != null ? this.bbcContext.getSdpContract().getContractAddress() : "",
                this.bbcContext.getSdpContract() != null ? this.bbcContext.getSdpContract().getStatus() : ""
        );

        return this.bbcContext;
    }

    @Override
    public CrossChainMessageReceipt readCrossChainMessageReceipt(String txHash) {
        getBBCLogger().info("BSN~ readCrossChainMessageReceipt txHash：{}",txHash);
        // 1. Obtain FISCO-BCOS receipt according to transaction hash
        TransactionReceipt transactionReceipt;

        try {
            transactionReceipt = client.getTransactionReceipt(txHash).getTransactionReceipt().get();
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to read cross chain message receipt (txHash: %s)", txHash
                    ), e
            );
        }

        // 2. Construct cross-chain message receipt
        CrossChainMessageReceipt crossChainMessageReceipt = getCrossChainMessageReceipt(transactionReceipt);
        getBBCLogger().info("cross chain message receipt for txhash {} : {}", txHash, JSON.toJSONString(crossChainMessageReceipt));

        return crossChainMessageReceipt;
    }

    private CrossChainMessageReceipt getCrossChainMessageReceipt(TransactionReceipt transactionReceipt) {
        getBBCLogger().info("BSN~ getCrossChainMessageReceipt transactionReceipt：{}",JSON.toJSONString(transactionReceipt));
        CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();
        if (transactionReceipt == null) {
            // If the transaction is not packaged, the return receipt is empty
            crossChainMessageReceipt.setConfirmed(false);
            crossChainMessageReceipt.setSuccessful(false);
            crossChainMessageReceipt.setTxhash("");
            crossChainMessageReceipt.setErrorMsg("");
            return crossChainMessageReceipt;
        }

        SDPMsg sdpMsg = SDPMsg.load(config.getSdpContractAddressDeployed(), client, keyPair);
        List<SDPMsg.ReceiveMessageEventResponse> receiveMessageEventResponses = sdpMsg.getReceiveMessageEvents(transactionReceipt);
        if (ObjectUtil.isNotEmpty(receiveMessageEventResponses)) {
            SDPMsg.ReceiveMessageEventResponse response = receiveMessageEventResponses.get(0);
            crossChainMessageReceipt.setConfirmed(true);
            crossChainMessageReceipt.setSuccessful(transactionReceipt.isStatusOK() && response.result);
            crossChainMessageReceipt.setTxhash(transactionReceipt.getTransactionHash());
            crossChainMessageReceipt.setErrorMsg(
                    transactionReceipt.isStatusOK() ? StrUtil.format(
                            "SDP calls biz contract: {}", response.result ? "SUCCESS" : response.errMsg
                    ) : StrUtil.emptyToDefault(transactionReceipt.getMessage(), "")
            );
            getBBCLogger().info(
                    "event receiveMessage from SDP contract is found in tx {} of block {} : " +
                            "( send_domain: {}, sender: {}, receiver: {}, biz_call: {}, err_msg: {} )",
                    transactionReceipt.getTransactionHash(), transactionReceipt.getBlockNumber(),
                    response.senderDomain, HexUtil.encodeHexStr(response.senderID), response.receiverID, response.result.toString(),
                    response.errMsg
            );
            return crossChainMessageReceipt;
        }

        crossChainMessageReceipt.setConfirmed(true);
        crossChainMessageReceipt.setSuccessful(transactionReceipt.isStatusOK());
        crossChainMessageReceipt.setTxhash(transactionReceipt.getTransactionHash());
        crossChainMessageReceipt.setErrorMsg(StrUtil.emptyToDefault(transactionReceipt.getMessage(), ""));

        return crossChainMessageReceipt;
    }

    public void myDemo() throws ABICodecException {

        TransactionDecoderInterface decoder =
                new TransactionDecoderService(client.getCryptoSuite());
        List<TransactionReceipt.Logs> logs = new ArrayList<>();
        TransactionReceipt.Logs logs1 = new TransactionReceipt.Logs();
        logs1.setAddress("0x7ba07500c98055c1169ff07f4f316877877a6eff");
        logs1.setData("0x0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000048800000019000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000343037337a727a79622e7469616e79616f686f6e6774752e636861696e000000ffffffff4631324246444437323835413130313141464433394131443341423300000000000000000000000000000000000000000000000000000000000003957b5c2273796d5c223a5c2231626566393866353136396361633333653138343430343730653336323030325c222c5c22736c78786a6d5c223a5c2230343232363130623635393133333033323433656232663466653933396138323930616532656139336636393534383734663365336133663764396534323032396563396333643263653131633332376530623162366137623835653538616436646232363962333131613661356463396238336234343363646466666534353838336163376635613236323831326366316434666630363566376431366465656335363465643334633235376262306565333365313965323164383536666531623139333561353139343334626536386263313838653330393764393036333530356430663833663738393133666661616162353164646266643661363538313734633864316638656336613530396661316132356361633263666635623530353737623563613237333431646166363235373365643538626533323339616565613334666531613466653237353833653634373762363536623366376332656561643634396431383133346434623233663261353332363533353265373965636263396361633230363238363834326564613233313635376638383238636334633633643831646566343636323331303966306465343533303462386263646234323638646639363233353033626262366662633032616236333164306638316438613465313365653030306439376663303265316336313830643531653134336564663631393731336431323532346561353334663532323136333365346264616137313434343932636333393433366432393432313533353932653839306234316361623861623339396530613561616537616461396431396662396134376539396162323432353733326635313963303931613361313334646366323635323133633063633731373139313366616536333762396131353466383563663139656431333365313631613331323763663663366637363338623933333832333666376139613165363538363138353562623338323334316234393361616564653838356531376534363166346536353265636138363764336131623032363632373361383663333034666635366636653130383935326562323463613463366666333533646238353964643863655c222c5c227977685c223a5c226c2b4559724d51596b54413553446d4461556c5937673d3d5c227d0000000000000000000000000000000000000000000000000000000000000000000000000000000000042400000000000000000000000000000000a297b33df32c39d5831f6ed26fbc8d8f71ed120900000001000000000000000000000000000000000000000000000000");
        logs.add(logs1);
        decoder.decodeEvents(AuthMsg.ABI_ARRAY[0], logs);
    }

    @Override
    public List<CrossChainMessage> readCrossChainMessagesByHeight(long height) {
        getBBCLogger().info("BSN~ readCrossChainMessagesByHeight:{}", height);
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }

        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())) {
            throw new RuntimeException("empty am contract in bbc context");
        }

        List<CrossChainMessage> messageList = ListUtil.toList();
        try {

            // 1. get block
            BcosBlock.Block block = client.getBlockByNumber(BigInteger.valueOf(height), false).getBlock();
                TransactionDecoderInterface decoder =
                        new TransactionDecoderService(client.getCryptoSuite());

                // 2. get crosschain msgs
            messageList.addAll(
                    // 2.1 get txHashes in block
                    block.getTransactions().stream()
                            .map(txHash -> {
                                // 2.2 get transaction receipt
                                TransactionReceipt receipt = client.getTransactionReceipt(txHash.get().toString()).getTransactionReceipt().get();
                                // 2.3 decode events from transaction receipt
                                Map<String, List<List<Object>>> events = null;
                                getBBCLogger().info("read cross chain receipt.getLogs: {}",receipt.getLogs());
                                try {
                                    events = decoder.decodeEvents(AuthMsg.ABI_ARRAY[0], receipt.getLogs());
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }


                                List<List<Object>> sendAuthMessage = events.getOrDefault("SendAuthMessage", Collections.emptyList());

                                return sendAuthMessage.stream()
                                        .map(event -> {
                                            String s = event.get(0).toString();
                                            getBBCLogger().info("read cross chain event.get(0): {}",s);
                                            /*byte[] decode = Base64.getDecoder().decode(s);
                                            getBBCLogger().info("read cross Base64.getDecoder()");*/
                                            byte[] decode = HexUtil.decodeHex(s);
                                            // 2.4 create crosschain msg
                                            return CrossChainMessage.createCrossChainMessage(
                                                    CrossChainMessage.CrossChainMessageType.AUTH_MSG,
                                                    Long.parseLong(receipt.getBlockNumber().substring(2), 16),
                                                    Long.parseLong(block.getTimestamp().substring(2), 16),
                                                    HexUtil.decodeHex(StrUtil.removePrefix(block.getHash().trim(), "0x")),
                                                    decode,
                                                    // todo: put ledger data, for SPV or other attestations (byte[]) event.get(0)
                                                    // this time we need no verify. it's ok to set it with empty bytes
                                                    "".getBytes(),
                                                    // todo: put ledger data, for SPV or other attestations
                                                    // this time we need no verify. it's ok to set it with empty bytes
                                                    "".getBytes(),
                                                    HexUtil.decodeHex(txHash.get().toString().replaceFirst("^0x", ""))
                                            );
                                        }).collect(Collectors.toList());
                            })
                            .flatMap(List::stream) // flatten from List<List<CrossChainMessage>> to List<CrossChainMessage>
                            .collect(Collectors.toList())
            );

            if (!messageList.isEmpty()) {
                getBBCLogger().info("read cross chain messages (height: {}, msg_size: {})", height, messageList.size());
                getBBCLogger().info("read cross chain messages (height: {}, msgs: {})",
                        height,
                        messageList.stream().map(JSON::toJSONString).collect(Collectors.joining(","))
                );
            }

            return messageList;
        } catch (Exception e) {
            getBBCLogger().info("failed to readCrossChainMessagesByHeight Exception: {}", e.getMessage());
            throw new RuntimeException(
                    String.format(
                            "failed to readCrossChainMessagesByHeight (Height: %d, contractAddr: %s, topic: %s)",
                            height,
                            this.bbcContext.getAuthMessageContract().getContractAddress(),
                            SENDAUTHMESSAGE_EVENT
                    ), e
            );
        }
    }

    @Override
    public Long queryLatestHeight() {
        Long l;
        try {
            if(client==null){
                getBBCLogger().info("queryLatestHeight client is null!");
            }
            BlockNumber blockNumber = client.getBlockNumber();
            if(blockNumber==null){
                getBBCLogger().info("queryLatestHeight blockNumber is null!");
            }
            BigInteger blockNumber1 = blockNumber.getBlockNumber();
            l = blockNumber1.longValue();
        } catch (Exception e) {
            e.printStackTrace();
            getBBCLogger().info("queryLatestHeight node: {}， Exception： {}", config.getConnectPeer(), e);
            throw new RuntimeException("failed to query latest height", e);
        }
        getBBCLogger().info("node: {}, latest height: {}", config.getConnectPeer(), l);
        return l;
    }

    @Override
    public long querySDPMessageSeq(String senderDomain, String senderID, String receiverDomain, String receiverID) {
        getBBCLogger().info("BSN~ querySDPMessageSeq:{} :{} :{} :{}", senderDomain,senderID,receiverDomain,receiverID);
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getSdpContract())) {
            throw new RuntimeException("empty sdp contract in bbc context");
        }

        // 2. load sdpMsg
        SDPMsg sdpMsg = SDPMsg.load(
                bbcContext.getSdpContract().getContractAddress(),
                this.client,
                this.keyPair
        );

        // 3. query sequence
        long seq;
        try {
            seq = Long.parseLong(sdpMsg.querySDPMessageSeq(
                    senderDomain,
                    HexUtil.decodeHex(senderID),
                    receiverDomain,
                    HexUtil.decodeHex(receiverID)
            ).getOutput().replace("0x", ""));

            getBBCLogger().info("sdpMsg seq: {} (senderDomain: {}, senderID: {}, receiverDomain: {}, receiverID: {})",
                    seq,
                    senderDomain,
                    senderID,
                    receiverDomain,
                    receiverID
            );
        } catch (Exception e) {
            throw new RuntimeException(String.format(
                    "failed to query sdpMsg seq (senderDomain: %s, senderID: %s, receiverDomain: %s, receiverID: %s)",
                    senderDomain,
                    senderID,
                    receiverDomain,
                    receiverID
            ), e);
        }

        return seq;
    }

    @Override
    public void setProtocol(String protocolAddress, String protocolType) {
        getBBCLogger().info("BSN~ setProtocol:{} :{}", protocolAddress,protocolType);
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())) {
            throw new RuntimeException("empty am contract in bbc context");
        }

        // 2. load am contract
        AuthMsg am = AuthMsg.load(
                this.bbcContext.getAuthMessageContract().getContractAddress(),
                this.client,
                this.keyPair
        );

        // 3. set protocol to am
        try {
            String owner = am.owner();
            TransactionReceipt receipt = am.setProtocol(protocolAddress, BigInteger.valueOf(Long.parseLong(protocolType)));
            //TransactionReceipt receipt = client.getTransactionReceipt("0xbebc4dcd64bb3848142f6771699a50fba2b1e3b2f26b3d2c223fdce41baf072b").getTransactionReceipt().get();
            getBBCLogger().info(
                    "set protocol result receipt: {}",
                    JSONObject.toJSONString(receipt)
            );
            int status;
            if(isHexadecimal(receipt.getStatus())){
                status= Integer.parseInt(receipt.getStatus().substring(2), 16); // 转换为10进制数
            }else {
                status= Integer.valueOf(receipt.getStatus());
            }

            getBBCLogger().info(
                    "set protocol (owner:{}, statusMsg: {}, message: {}) ",owner,
                    receipt.getStatusMsg(), receipt.getMessage()
            );

            if (status == 0) {
                getBBCLogger().info(
                        "set protocol (address: {}, type: {}) to AM {} by tx {} ",
                        protocolAddress, protocolType,
                        this.bbcContext.getAuthMessageContract().getContractAddress(),
                        receipt.getTransactionHash()
                );
            } else {
                getBBCLogger().info(
                        "set protocol failed, receipt status code: {}",
                        receipt.getStatus()
                );
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to set protocol (address: %s, type: %s) to AM %s",
                            protocolAddress, protocolType, this.bbcContext.getAuthMessageContract().getContractAddress()
                    ), e
            );
        }

        // 4. update am contract status
        try {
            if (!StrUtil.isEmpty(am.getProtocol(BigInteger.ZERO))) {
                this.bbcContext.getAuthMessageContract().setStatus(ContractStatusEnum.CONTRACT_READY);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to update am contract status (address: %s)",
                            this.bbcContext.getAuthMessageContract().getContractAddress()
                    ), e);
        }
    }

    @Override
    public CrossChainMessageReceipt relayAuthMessage(byte[] rawMessage) {
        getBBCLogger().info("BSN~ relayAuthMessage:{}", HexUtil.encodeHexStr(rawMessage));
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getAuthMessageContract())) {
            throw new RuntimeException("empty am contract in bbc context");
        }

        getBBCLogger().info("relay AM {} to {} ",
                HexUtil.encodeHexStr(rawMessage), this.bbcContext.getAuthMessageContract().getContractAddress());

        // 2. creat Transaction
        CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();
        try {
            // 2.1 async send tx
            TransactionResponse response =
                    transactionProcessorAM.sendTransactionAndGetResponse(
                            this.bbcContext.getAuthMessageContract().getContractAddress(),
                            AuthMsg.ABI_ARRAY[0],
                            AuthMsg.FUNC_RECVPKGFROMRELAYER,
                            Collections.singletonList(rawMessage));

            crossChainMessageReceipt.setConfirmed(response.getTransactionReceipt().isStatusOK());
            crossChainMessageReceipt.setSuccessful(response.getTransactionReceipt().isStatusOK());
            crossChainMessageReceipt.setTxhash(response.getTransactionReceipt().getTransactionHash());
            crossChainMessageReceipt.setErrorMsg(response.getReceiptMessages());

            getBBCLogger().info("relay AM to {} result: {}, {} ",
                    this.bbcContext.getAuthMessageContract().getContractAddress(),
                    crossChainMessageReceipt.isSuccessful(),
                    crossChainMessageReceipt.getErrorMsg());

            // 2.2 return crossChainMessageReceipt
            return crossChainMessageReceipt;
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("failed to relay AM %s to %s",
                            HexUtil.encodeHexStr(rawMessage), this.bbcContext.getAuthMessageContract().getContractAddress()
                    ), e
            );
        }
    }

    @Override
    public void setupAuthMessageContract() {
        getBBCLogger().info("BSN~ setupAuthMessageContract");
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

        // 2. deploy contract
        AuthMsg authMsg;
        try {
            authMsg = AuthMsg.deploy(client, keyPair);
        } catch (Exception e) {
            throw new RuntimeException("failed to deploy authMsg", e);
        }

        // 3. get tx receipt
        TransactionReceipt transactionReceipt = authMsg.getDeployReceipt();

        // 4. check whether the deployment is successful
        if (!ObjectUtil.isNull(transactionReceipt) && StrUtil.equals(transactionReceipt.getStatus(), TX_RECEIPT_STATUS_SUCCESS)) {
            AuthMessageContract authMessageContract = new AuthMessageContract();
            authMessageContract.setContractAddress(authMsg.getContractAddress());
            authMessageContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
            bbcContext.setAuthMessageContract(authMessageContract);
            getBBCLogger().info("setup am contract successful: {}", authMsg.getContractAddress());
        } else {
            throw new RuntimeException("failed to get deploy authMsg tx receipt");
        }
    }

    @Override
    public void setupSDPMessageContract() {
        getBBCLogger().info("BSN~ setupSDPMessageContract");
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

        // 2. deploy contract
        SDPMsg sdpMsg;
        try {
            sdpMsg = SDPMsg.deploy(client, keyPair);
        } catch (Exception e) {
            throw new RuntimeException("failed to deploy sdpMsg", e);
        }

        // 3. get tx receipt
        TransactionReceipt transactionReceipt = sdpMsg.getDeployReceipt();

        // 4. check whether the deployment is successful
        if (!ObjectUtil.isNull(transactionReceipt) && StrUtil.equals(transactionReceipt.getStatus(), TX_RECEIPT_STATUS_SUCCESS)) {
            SDPContract sdpContract = new SDPContract();
            sdpContract.setContractAddress(sdpMsg.getContractAddress());
            sdpContract.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
            bbcContext.setSdpContract(sdpContract);
            getBBCLogger().info("setup sdp contract successful: {}", sdpMsg.getContractAddress());
        } else {
            throw new RuntimeException("failed to get deploy sdpMsg tx receipt");
        }
    }

    @Override
    public void setAmContract(String contractAddress) {
        getBBCLogger().info("BSN~ setAmContract  contractAddress：{}",contractAddress);
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (ObjectUtil.isNull(this.bbcContext.getSdpContract())) {
            throw new RuntimeException("empty sdp contract in bbc context");
        }

        // 2. load sdp contract
        SDPMsg sdp = SDPMsg.load(
                this.bbcContext.getSdpContract().getContractAddress(),
                this.client,
                this.keyPair
        );

        // 3. set am to sdp
        try {
            TransactionReceipt receipt = sdp.setAmContract(contractAddress);
            int status;
            if(isHexadecimal(receipt.getStatus())){
                status= Integer.parseInt(receipt.getStatus().substring(2), 16); // 转换为10进制数
            }else {
                status= Integer.valueOf(receipt.getStatus());
            }
            if (status == 0) {
                getBBCLogger().info(
                        "set am contract (address: {}) to SDP {} by tx {}",
                        contractAddress,
                        this.bbcContext.getSdpContract().getContractAddress(),
                        receipt.getTransactionHash()
                );
            } else {
                getBBCLogger().info(
                        "set am contract failed, receipt status code: {}",
                        receipt.getStatus()
                );
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to set am contract (address: %s) to SDP %s",
                            contractAddress,
                            this.bbcContext.getSdpContract().getContractAddress()
                    ), e
            );
        }

        // 4. update sdp contract status
        try {
            if (!StrUtil.isEmpty(sdp.getAmAddress()) && !isByteArrayZero(sdp.getLocalDomain())) {
                this.bbcContext.getSdpContract().setStatus(ContractStatusEnum.CONTRACT_READY);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to update sdp contract status (address: %s)",
                            this.bbcContext.getSdpContract().getContractAddress()
                    ), e);
        }
    }

    private boolean isByteArrayZero(byte[] bytes) {
        for (byte b : bytes) {
            if (b != 0x00) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setLocalDomain(String domain) {
        getBBCLogger().info("BSN~ setLocalDomain  domain：{}",domain);
        // 1. check context
        if (ObjectUtil.isNull(this.bbcContext)) {
            throw new RuntimeException("empty bbc context");
        }
        if (StrUtil.isEmpty(this.bbcContext.getSdpContract().getContractAddress())) {
            throw new RuntimeException("none sdp contract address");
        }

        // 2. load sdp contract
        SDPMsg sdp = SDPMsg.load(
                this.bbcContext.getSdpContract().getContractAddress(),
                this.client,
                this.keyPair
        );

        // 3. set domain to sdp

        try {
            String owner = sdp.owner();
            TransactionReceipt receipt = sdp.setLocalDomain(domain);
            //TransactionReceipt receipt = client.getTransactionReceipt("0x3661887e7ae9be48a79485aebbc46a174cb00897e84d52628fbab8c9bce97270").getTransactionReceipt().get();
            int status;
            if(isHexadecimal(receipt.getStatus())){
                // 转换为10进制数

                status= Integer.parseInt(receipt.getStatus().substring(2), 16);
            }else {
                status= Integer.valueOf(receipt.getStatus());
            }
            getBBCLogger().info(
                    "set setLocalDomain (owner:{}, statusMsg: {}, message: {}) ", owner,
                    receipt.getStatusMsg(), receipt.getMessage()
            );
            getBBCLogger().info(
                    "set setLocalDomain result receipt: {}",
                    JSONObject.toJSONString(receipt)
            );
            if (status == 0) {
                getBBCLogger().info(
                        "set domain ({}) to SDP {} by tx {}",
                        domain,
                        this.bbcContext.getSdpContract().getContractAddress(),
                        receipt.getTransactionHash()
                );
            } else {
                getBBCLogger().info(
                        "set domain failed, receipt status code: {}",
                        receipt.getStatus()
                );
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to set domain (%s) to SDP %s",
                            domain,
                            this.bbcContext.getSdpContract().getContractAddress()
                    ), e
            );
        }

        // 4. update sdp contract status
        try {
            if (!StrUtil.isEmpty(sdp.getAmAddress()) && !ObjectUtil.isEmpty(sdp.getLocalDomain())) {
                this.bbcContext.getSdpContract().setStatus(ContractStatusEnum.CONTRACT_READY);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "failed to update sdp contract status (address: %s)",
                            this.bbcContext.getSdpContract().getContractAddress()
                    ), e);
        }
    }



    public  boolean isHexadecimal(String str) {
        if(str.startsWith("0x")){
            return true;
        }else{
            return false;
        }
    }

}