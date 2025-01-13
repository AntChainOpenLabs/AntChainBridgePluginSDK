package com.alipay.antchain.bridge.plugins.fabric;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.commons.core.sdp.ISDPMessage;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageFactory;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset;
import org.hyperledger.fabric.protos.peer.FabricProposal;
import org.hyperledger.fabric.protos.peer.FabricProposalResponse;
import org.hyperledger.fabric.protos.peer.FabricTransaction;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hyperledger.fabric.sdk.BlockInfo.EnvelopeType.TRANSACTION_ENVELOPE;
import static org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions;

public class Fabric14Client {

    private int timeout = 3 * 1000;

    private final Logger logger;
    static final String FABRIC_PEER_NODE_VERSION = "node_version";
    static final String FABRIC_SERVICE_DISCOVERY_KEY = "discover";

    private static String FABRIC_AM_WRITE_KEY_PREFIX = "oraclelogic_crosschain_msg_";
    private static String FABRIC_JSON_CHAINCODE_SECTION = "chaincode";
    private static String FABRIC_JSON_CHAINCODE_NAME = "name";
    private static String FABRIC_CC_FN_OUTER_ADMIN_MANAGE = "oracleAdminManage";
    private static String FABRIC_CC_FN_OUTER_RECV_MESSAGE = "recvMessage";

    // Fabric跨链链码内层函数名
    private static String FABRIC_CC_FN_INNER_QUERY_RECV_P2P_MSG_SEQ = "queryRecvP2PMsgSeq";
    private static String FABRIC_CC_FN_INNER_QUERY_SHA256_INVERT = "querySha256Invert";
    private static String FABRIC_CC_FN_INNER_REGISTER_SHA256_INVERT = "registerSha256Invert";
    private static String FABRIC_JSON_CHAINCODE_VERSION = "version";
    private static String FABRIC_JSON_CHAINCODE_PATH = "path";

    // Fabric
    private HFClient hfClient;
    private Channel channel;
    private Collection<Peer> validatorPeers;
    private List<String> validatorPeersName;
    private Set<String> chaincodesOnPeers;

    private ChaincodeID chaincodeID = null;
    private JSONObject orgConfig;


    public Fabric14Client(String hfClientConfig, Logger logger) {
        orgConfig = JSONObject.parseObject(hfClientConfig);
        this.validatorPeers = new ArrayList<>();
        this.validatorPeersName = new ArrayList<>();
        this.chaincodesOnPeers = new HashSet<>();

        // construct chaincodeId
        String name = ((JSONObject)orgConfig
                .get(this.FABRIC_JSON_CHAINCODE_SECTION))
                .getString(this.FABRIC_JSON_CHAINCODE_NAME);
        String version =  ((JSONObject)orgConfig
                .get(this.FABRIC_JSON_CHAINCODE_SECTION))
                .getString(this.FABRIC_JSON_CHAINCODE_VERSION);
        String codePath = ((JSONObject)orgConfig
                .get(this.FABRIC_JSON_CHAINCODE_SECTION))
                .getString(this.FABRIC_JSON_CHAINCODE_PATH);

        logger.info("chaincode, name: {}, version: {}, codePath: {}", name, version, codePath);
        ChaincodeID.Builder chaincodeIdBuilder = ChaincodeID.newBuilder().setName(name)
                .setVersion(version)
                .setPath(codePath);
        chaincodeID =  chaincodeIdBuilder.build();
        this.logger = logger;
    }

    public User getFabricUser() {
        return this.hfClient.getUserContext();
    }

    public boolean start() {
        logger.info("client begin to start");
        // Construct hfClient and channel
        hfClient = HFClient.createNewInstance();

        try {
            hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

            channel = reconstructChannel();

            channel.initialize();

            Collection<Peer> peers = channel.getPeers();
            if (!ifDiscoveryAvailable()) {
                for (Peer peer : peers) {
                    try {
                        channel.queryInstantiatedChaincodes(peer)
                                .forEach(chaincodeInfo -> chaincodesOnPeers.add(chaincodeInfo.getName()));
                    } catch(Exception e) {
                        logger.warn("queryInstantiatedChaincodes from peer {} failed: ", peer.getUrl(), e);
                    }
                }
            }

            List<Peer> peers_validator = new ArrayList<>();
            peers.forEach(peer -> {
                if (validatorPeersName.contains(peer.getName())) {
                    peers_validator.add(peer);
                }
            });
            validatorPeers = peers_validator;

        } catch (Exception e) {
            logger.error("fabric client, start failed: ", e);
            return false;
        }


        logger.info("client end to start");
        return true;
    }

    private Channel reconstructChannel() throws Exception {

        String name = ((JSONObject)((JSONObject)orgConfig).get("channel")).getString("name");

        String userName = ((JSONObject)((JSONObject)orgConfig).get("user")).getString("name");
        String mspId = ((JSONObject)((JSONObject)orgConfig).get("user")).getString("mspId");
        String userKey = ((JSONObject)((JSONObject)orgConfig).get("user")).getString("key");
        String userCert = ((JSONObject)((JSONObject)orgConfig).get("user")).getString("cert");

        Fabric14User user = new Fabric14User(userName);
        user.setMspId(mspId);
        user.setEnrollment(userKey, userCert);

        hfClient.setUserContext(user);

        Channel newChannel = hfClient.newChannel(name);

        for (Object peerConf : ((JSONObject)orgConfig).getJSONArray("discoveryPeers")) {
            String peerName = ((JSONObject)peerConf).getString("name");
            String peerLocation = ((JSONObject)peerConf).getString("peerLocation");
            Object peerProperties = ((JSONObject)peerConf).get("peerProperties");

            Properties properties = new Properties();
            if (null != peerProperties) {
                for (String key : ((JSONObject)peerProperties).keySet()) {
                    if (key.contains("Bytes")) {
                        ((Hashtable<Object, Object>)properties).put(key,
                                ((JSONObject)peerProperties).getString(key).getBytes());
                    } else {
                        properties.setProperty(key, ((JSONObject)peerProperties).getString(key));
                    }
                }
            }

            properties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 100000000);

            final Channel.PeerOptions peerEventingOptions;
            // nothing set means SD on
            if (BooleanUtil.toBoolean(properties.getProperty(FABRIC_SERVICE_DISCOVERY_KEY, "true"))) {
                peerEventingOptions = createPeerOptions().registerEventsForFilteredBlocks().setPeerRoles(
                        EnumSet.of(Peer.PeerRole.SERVICE_DISCOVERY, Peer.PeerRole.LEDGER_QUERY, Peer.PeerRole.CHAINCODE_QUERY, Peer.PeerRole.EVENT_SOURCE));
            } else {
                peerEventingOptions = createPeerOptions().registerEventsForFilteredBlocks().setPeerRoles(
                        EnumSet.of(Peer.PeerRole.ENDORSING_PEER, Peer.PeerRole.LEDGER_QUERY, Peer.PeerRole.CHAINCODE_QUERY, Peer.PeerRole.EVENT_SOURCE));
            }

            Peer peer = hfClient.newPeer(peerName, peerLocation, properties);
            logger.info("add new peer, name: [{}], location: [{}]", peerName, peerLocation);
            newChannel.addPeer(peer, peerEventingOptions);
        }

        JSONArray orderConfArray = ((JSONObject)orgConfig).getJSONArray("orderers");
        if (null != orderConfArray && !orderConfArray.isEmpty()) {
            for (Object orderConf : orderConfArray) {
                String orderName = ((JSONObject)orderConf).getString("name");
                String orderLocation = ((JSONObject)orderConf).getString("ordererLocation");
                JSONObject orderProperties = ((JSONObject)orderConf).getJSONObject("ordererProperties");

                Properties properties = new Properties();
                if (null != orderProperties) {
                    for (String key : orderProperties.keySet()) {
                        if (key.contains("Bytes")) {
                            properties.put(key, orderProperties.getString(key).getBytes());
                        } else {
                            properties.setProperty(key, orderProperties.getString(key));
                        }
                    }
                }

                Orderer orderer = hfClient.newOrderer(orderName, orderLocation, properties);
                logger.info("add new order, name: [{}], location: [{}]", orderName, orderLocation);
                newChannel.addOrderer(orderer);
            }
        }

        for (Object peerConf : ((JSONObject)orgConfig).getJSONArray("validatorPeers")) {
            String peerName = ((JSONObject)peerConf).getString("name");
            this.validatorPeersName.add(peerName);
        }

        // 配置
        String runningtls = (((JSONObject)orgConfig).getString("runningtls"));
        if ("false".equals(runningtls)) {
            Properties sdprops = new Properties();
            sdprops.put("org.hyperledger.fabric.sdk.discovery.default.protocol", "grpc:");
            newChannel.setServiceDiscoveryProperties(sdprops);
        }

        return newChannel;
    }

    public boolean shutdown() {
        if (channel.isShutdown()) {
            return true;
        }
        channel.shutdown(true);
        return true;
    }

    public boolean ifHasDeployedFabricChaincode() {
        return true;
    }

    public boolean ifHasDeployedAMClientContract() {
        return true;
    }

    public boolean ifAccountValid() {
        return true;
    }

    public BlockInfo getNativeBlockByHeight(long height) {
        try {
            BlockInfo blockInfo = channel.queryBlockByNumber(height);
            return blockInfo;

        } catch (Exception e) {
            logger.error("Exception at Fabric14Client getNativeBlockByHeight.", e);
            logger.error("fabric client, get native blockinfo error: ", e);
            return null;
        }
    }

    public List<CrossChainMessage> readCrossChainMessageReceipt(long height) {
        if (this.chaincodeID == null) {
            logger.info("[FabricBBCService] chaincode is not found {}");
            throw new RuntimeException("chaincode is not found");
        }
        BlockInfo blockInfo = getNativeBlockByHeight(height);
        List<CrossChainMessage> cmlist = Lists.newArrayList();

        if (blockInfo == null) {
            logger.warn("FabricFabricChaincode - got authmessage from block ({}) failed", height);
            return cmlist;
        }

        logger.info("FabricFabricChaincode - parse authmessage from block ({})", height);
        int i = 0;
        int j = 0;
        for (BlockInfo.EnvelopeInfo envelopeInfo : blockInfo.getEnvelopeInfos()) {
            ++i;
            logger.info("FabricFabricChaincode - got envelope({}), type ({}), isValid {}",
                    i, envelopeInfo.getType(), envelopeInfo.isValid() ? "true" : "false");
            if (!envelopeInfo.isValid()) {
                continue;
            }
            if (envelopeInfo.getType() == TRANSACTION_ENVELOPE) {
                BlockInfo.TransactionEnvelopeInfo transactionEnvelopeInfo
                        = (BlockInfo.TransactionEnvelopeInfo)envelopeInfo;

                int action_index = -1;
                // Range actions of transaction

                for (BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo transactionActionInfo :
                        transactionEnvelopeInfo
                                .getTransactionActionInfos()) {
                    action_index++;
                    ++j;
                    String chaincodeIDName = transactionActionInfo.getChaincodeIDName();
                    String chaincodeIDVersion = transactionActionInfo.getChaincodeIDVersion();
                    String chaincodeIDPath = transactionActionInfo.getChaincodeIDPath();

                    logger.info("FabricFabricChaincode - got action({}) in envelope({}), chaincode name: {}, chaincode version: {}, chaincode path: {}",
                            j, i, chaincodeIDName, chaincodeIDVersion, chaincodeIDPath);

                    {
                        TxReadWriteSetInfo rwinfo = transactionActionInfo.getTxReadWriteSet();
                        for (TxReadWriteSetInfo.NsRwsetInfo rwset : rwinfo.getNsRwsetInfos()) {
                            try {
                                logger.info("namespace {}", rwset.getNamespace());
                                if (rwset.getNamespace().equals(chaincodeID.getName())) {
                                    KvRwset.KVRWSet kv = rwset.getRwset();
                                    for (KvRwset.KVWrite w : kv.getWritesList()) {
                                        logger.info("key {}", w.getKey());
                                        if (w.getKey().startsWith(this.FABRIC_AM_WRITE_KEY_PREFIX)) {
                                            try {
                                                // 无序hex decode
                                                byte[] amBytes = w.getValue().toByteArray();
                                                CrossChainMessage cm = CrossChainMessage.createCrossChainMessage(CrossChainMessage.CrossChainMessageType.AUTH_MSG,
                                                        blockInfo.getBlockNumber(), System.currentTimeMillis(), blockInfo.getDataHash(), amBytes,
                                                        "".getBytes(),
                                                        "".getBytes(),
                                                        HexUtil.decodeHex(transactionEnvelopeInfo.getTransactionID())
                                                );
                                                logger.info("crosschain message log, block height {} transaction id {}", blockInfo.getBlockNumber(), transactionEnvelopeInfo.getTransactionID());
                                                cmlist.add(cm);
                                            } catch (Exception e) {
                                                logger.warn("crosschain message log exception", e);
                                                continue;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.warn("crosschain message log exception", e);
                                continue;
                            }
                        }
                    }

                }
            }
        }
        return cmlist;
    }

    /**
     * 如果有Discovery的节点，返回True.
     * TODO：如果peers中有SERVICE_DISCOVERY，也有不支持的，这种情况还需要仅有一步探讨。
     * @return
     */
    private boolean ifDiscoveryAvailable() {
        return channel.getPeers(EnumSet.of(Peer.PeerRole.SERVICE_DISCOVERY)).size() > 0;
    }

    public Set<String> getChaincodesOnPeers() {
        return chaincodesOnPeers;
    }

    HFClient getClient() {
        return hfClient;
    }

    public long getLastBlockHeight() {
        long height = 0;
        try {
            // for debug
            Collection<Peer> peers = channel.getPeers();
            for (Peer peer : peers) {
               logger.info("peer name: {}, peer url: {}", peer.getName(), peer.getUrl());
            }
            BlockchainInfo channelInfo = channel.queryBlockchainInfo();
            height = channelInfo.getHeight();
            // fabric区块链从0开始计数
            height = height - 1;
            logger.debug("fabric client, query block height, value: {}", height);
        } catch (Exception e) {
            // TODO: error
            logger.error("Exception at Fabric14Client getLastBlockHeight.", e);
            throw new RuntimeException(
                    format("Exception at Fabric14Client getLastBlockHeight."), e);
        }

        return height;
    }

    public long querySDPMessageSeq(String senderDomain, String from, String to) {
        logger.info("[FabricBBCService] query SDP message seq for" +
                        "senderDomain: {}, fromName: {}, toName: {}",
                senderDomain,
                from,
                to);

        ArrayList<String> args = new ArrayList<String>();
        args.add(this.FABRIC_CC_FN_INNER_QUERY_RECV_P2P_MSG_SEQ);
        args.add(senderDomain);
        args.add(from);
        args.add(to);

        logger.info("FabricChaincode - query queryP2PMsgSeq on chaincode");
        String res = this.chaincodeQuery(this.FABRIC_CC_FN_OUTER_ADMIN_MANAGE, args);
        if (res == null) {
            logger.info("FabricChaincode - query queryP2PMsgSeq result: {}", res);
            return 0;
        }
        JSON resObj = JSON.parseObject(res);
        long ret = Long.parseLong(((JSONObject)resObj).getString("result"));
        logger.info("FabricChaincode - query queryP2PMsgSeq result: {}", ret);
        return ret;
    }

    public ChaincodeID getOralceChaincodeId() {
        return chaincodeID;
    }

    private String chaincodeQuery(String fn, ArrayList<String> args) {
        ChaincodeID chaincodeId = this.getOralceChaincodeId();

        try {

            QueryByChaincodeRequest queryByChaincodeRequest = hfClient.newQueryProposalRequest();
            queryByChaincodeRequest.setArgs(args);
            queryByChaincodeRequest.setFcn(fn);
            queryByChaincodeRequest.setChaincodeID(chaincodeId);

            Collection<ProposalResponse> queryProposals = channel.queryByChaincode(queryByChaincodeRequest);
            Collection<ProposalResponse> successful = new ArrayList<>();
            for (ProposalResponse proposalResponse : queryProposals) {
                if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
                    logger.info(
                            "FabricChaincode - Failed query proposal from peer {}\nstatus: {}\nMessages: {}\nWas verified: {}",
                            proposalResponse.getPeer().getName(), proposalResponse.getStatus(),
                            proposalResponse.getMessage(), proposalResponse.isVerified());
                    continue;
                }
                successful.add(proposalResponse);
                break;
            }

            if (successful.isEmpty()) {
                return null;
            }

            String payload = successful.iterator().next().getProposalResponse().getResponse().getPayload()
                    .toStringUtf8();
            logger.info("FabricChaincode - chaincodeQuery response payload: {}", payload);
            return payload;
        } catch (Exception e) {
            logger.error("Exception at FabricChaincode chaincodeQuery.", e);
            throw new RuntimeException(
                    format("Exception at FabricChaincode chaincodeQuery. fn %s", fn),
                    e);
        }
    }

    private CrossChainMessageReceipt chaincodeInvoke(String fn, ArrayList<String> args, Map<String, byte[]> trans, ArrayList<String> cc_interest) {
        ChaincodeID chaincodeId = this.getOralceChaincodeId();
        return chaincodeInvokeBase(chaincodeId, fn, args, trans, cc_interest, timeout);
    }

    public void setLocalDomain(String localDomain) {
        ArrayList<String> args = new ArrayList<>();
        Map<String, byte[]> trans = new HashMap<>();
        ArrayList<String> cc_interest = new ArrayList<>();

        args.add("setExpectedDomain");
        args.add(localDomain);
        trans.clear();
        cc_interest.clear();
        CrossChainMessageReceipt receipt = chaincodeInvoke("oracleAdminManage", args, trans, cc_interest);
        logger.info("[FabricBBCService]set local domain {}, isSuccess {}, reason {}", localDomain, receipt.isSuccessful(), receipt.getErrorMsg());
    }

    public CrossChainMessageReceipt chaincodeInvokeBase(ChaincodeID chaincodeID, String fn, ArrayList<String> args, Map<String, byte[]> trans, ArrayList<String> cc_interest, int timeout) {



        TransactionProposalRequest transactionProposalRequest = hfClient.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeID(chaincodeID);
        transactionProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
        transactionProposalRequest.setFcn(fn);
        // TODO: read from config
        transactionProposalRequest.setProposalWaitTime(
                3 * 1000
        );
        transactionProposalRequest.setArgs(args);

        try {
            transactionProposalRequest.setTransientMap(trans);
        } catch (Exception e) {
            throw new RuntimeException(
                    format("Fabric chaincode invoke failed with %s exception %s", e.getClass().getName(), e.getMessage()),
                    e);
        }

        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();

        try {

            Collection<ProposalResponse> transactionPropResp;
            // if (ifDiscoveryAvailable()) {
            //     Channel.DiscoveryOptions options = Channel.DiscoveryOptions.createDiscoveryOptions();
            //     for (String cc : cc_interest) {
            //         options.setServiceDiscoveryChaincodeInterests(
            //                 Channel.ServiceDiscoveryChaincodeCalls.createServiceDiscoveryChaincodeCalls(cc));
            //     }
            //     transactionPropResp = channel.sendTransactionProposalToEndorsers(
            //             transactionProposalRequest, options);
            // } else {
            //     transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest);
            // }
            transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());

            for (ProposalResponse response : transactionPropResp) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    logger.info("FabricChaincode - Successful transaction proposal response Txid: {} from peer {}",
                            response.getTransactionID(),
                            response.getPeer().getName());
                    successful.add(response);

                    FabricProposal.ChaincodeAction action
                            = FabricProposal.ChaincodeAction.parseFrom(
                            FabricProposalResponse.ProposalResponsePayload.parseFrom(
                                    response.getProposalResponse().getPayload()).getExtension()
                    );
                    logger.info("chain code", action.getChaincodeId());
                } else {
                    failed.add(response);
                    logger.info("failed code {}, reason: {}", response.getStatus(), response.getMessage());
                }
            }

            logger.info(
                    "FabricChaincode - Received {} transaction proposal responses. Successful+verified: {} . Failed: {}",
                    transactionPropResp.size(), successful.size(), failed.size());
            if (failed.size() > 0) {
                ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
                logger.warn("FabricChaincode - Not enough endorsers for invoke: " + failed.size() + " endorser error: " +
                        firstTransactionProposalResponse.getMessage() +
                        ". Was verified: " + firstTransactionProposalResponse.isVerified());
                CrossChainMessageReceipt ret = new CrossChainMessageReceipt();
                ret.setTxhash("");
                ret.setSuccessful(false);
                ret.setConfirmed(false);
                ret.setErrorMsg("Not enough endorsers for invoke");
                return ret;
            }

            Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils.getProposalConsistencySets(
                    transactionPropResp);
            if (proposalConsistencySets.size() != 1) {
                logger.info(format("FabricChaincode - Expected only one set of consistent proposal responses but got %d",
                        proposalConsistencySets.size()));
                CrossChainMessageReceipt ret = new CrossChainMessageReceipt();
                ret.setTxhash("");
                ret.setSuccessful(false);
                ret.setConfirmed(false);
                ret.setErrorMsg("Inconsistent proposal responses");
                return ret;
            }

            logger.info("FabricChaincode - Successfully received transaction proposal responses.");

            final StringBuilder evenTransactionId = new StringBuilder();
            channel.sendTransaction(transactionPropResp).thenApply(transactionEvent -> {
                evenTransactionId.setLength(0);
                evenTransactionId.append(transactionEvent.getTransactionID());
                return null;
            }).exceptionally(e -> {
                if (e instanceof TransactionEventException) {
                    BlockEvent.TransactionEvent te = ((TransactionEventException)e).getTransactionEvent();
                    if (te != null) {
                        throw new RuntimeException(
                                format("Fabric invoke Transaction with txid %s failed. %s", te.getTransactionID(),
                                        e.getMessage()), e);
                    }
                }
                throw new RuntimeException(format("Fabric chaincode invoke failed with %s exception %s", e.getClass().getName(), e.getMessage()), e);
            }).get(timeout, TimeUnit.SECONDS);

            // String txid = ((LinkedList<ProposalResponse>) successful).getFirst().getTransactionID();
            String txid = evenTransactionId.toString();
            logger.info("FabricChaincode - send tx success, txid: {}", txid);
            CrossChainMessageReceipt ret = new CrossChainMessageReceipt();
            ret.setTxhash(txid);
            ret.setSuccessful(true);
            ret.setConfirmed(false);
            ret.setErrorMsg("");
            return ret;
        } catch (Exception e) {
            logger.error("Exception at FabricChaincode chaincodeInvokeBase.", e);
            logger.error("FabricChaincode - send tx failed: ", e);
            CrossChainMessageReceipt ret = new CrossChainMessageReceipt();
            ret.setTxhash("");
            ret.setSuccessful(false);
            ret.setConfirmed(false);
            ret.setErrorMsg("invoke chaincode unknown error " + e);
            return ret;
        }
    }

    private String queryChaincodeName(String ccHex) {
        ArrayList<String> args = new ArrayList<String>();
        args.add(this.FABRIC_CC_FN_INNER_QUERY_SHA256_INVERT);
        args.add(ccHex);

        logger.info("FabricChaincode - query query chaincode name {}", ccHex);
        String res = this.chaincodeQuery(this.FABRIC_CC_FN_OUTER_ADMIN_MANAGE, args);
        if (res == null) {
            logger.info("FabricChaincode - query query chaincode name result: {}", res);
            return "";
        }
        return res;
    }

    public Collection<String> getDiscoveredChaincodeNames() {
        return channel.getDiscoveredChaincodeNames();
    }

    private boolean registerChaincodeName(String chaincode_hash_hex) {

        Collection<String> cc_names = channel.getDiscoveredChaincodeNames();
        if (cc_names.size() == 0) {
            cc_names = getChaincodesOnPeers();
        }
        String found_cc = "";
        for (String cc_name : cc_names) {
            try {
                String found_hash_hex_cc = Hex.encodeHexString(DigestUtil.sha256(cc_name.getBytes()));
                if (Hex.encodeHexString(DigestUtil.sha256(cc_name.getBytes())).equals(chaincode_hash_hex)) {
                    found_cc = cc_name;
                    break;
                }

            } catch (Exception e) {
                logger.warn("can not found chaincode", e);
                continue;
            }
        }

        if (found_cc.isEmpty()) {
            logger.info("do not found target domain {}", chaincode_hash_hex);
            return false;
        }

        ArrayList<String> args = new ArrayList<String>();
        ArrayList<String> cc_interest = new ArrayList<String>();
        Map<String, byte[]> trans = new HashMap<>();
        args.add(this.FABRIC_CC_FN_INNER_REGISTER_SHA256_INVERT);
        args.add(found_cc);

        logger.info("FabricChaincode - register chaincode name ");
        // TODO: read from config
        // int timeout = ServerContext.getInstance()
        //         .getLocalConfig()
        //         .getInt(LocalConfig.FABRIC_INVOKE_TIMEOUT_IN_SECONDS_FOR_ORDERED_MSG, INVOKE_TIMEOUT_IN_SECONDS_FOR_ORDERED_MSG);
        CrossChainMessageReceipt receipt =
                this.chaincodeInvoke(this.FABRIC_CC_FN_OUTER_ADMIN_MANAGE, args, trans, cc_interest);
        logger.info("FabricChaincode - register chaincode name, resutl:{}", receipt.isSuccessful());
        return receipt.isSuccessful();
    }

    public CrossChainMessageReceipt recvPkgFromRelayer(byte[] udagProofPkg) {

        ByteArrayInputStream stream = new ByteArrayInputStream(udagProofPkg);

        byte[] zeros = new byte[4];
        stream.read(zeros, 0, 4);

        byte[] rawLen = new byte[4];
        stream.read(rawLen, 0, 4);

        int len = ByteUtil.bytesToInt(rawLen, ByteOrder.BIG_ENDIAN);

        byte[] rawProof = new byte[len];
        stream.read(rawProof, 0, len);

        MockProof proof = TLVUtils.decode(rawProof, MockProof.class);
        IAuthMessage authMessage = AuthMessageFactory.createAuthMessage(proof.getResp().getRawResponse());
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(authMessage.getPayload());

        // 可以Fabric链上反查sha256，找到接收消息的biz链码
        String cc_hash_hex = sdpMessage.getTargetIdentity().toHex();
        String recv_chaincodeName = this.queryChaincodeName(cc_hash_hex);

        if (recv_chaincodeName.isEmpty()) {
            // 如果chaincode还没注册，则搜索channel，注册上联
            boolean res = this.registerChaincodeName(cc_hash_hex);
            if (!res) {
                logger.warn("register chaincode failed");
                CrossChainMessageReceipt ret = new CrossChainMessageReceipt();
                ret.setTxhash("");
                ret.setSuccessful(false);
                ret.setConfirmed(false);
                ret.setErrorMsg("recv chaincode not found");
                return ret;
            }
        }

        ArrayList<String> args = new ArrayList<>();
        ArrayList<String> cc_interest = new ArrayList<>();
        Map<String, byte[]> trans = new HashMap<>();
        // service id is useless
        args.add("");
        args.add(HexUtil.encodeHexStr(udagProofPkg)); // 只提交proofs
        // 感兴趣的chaincode
        cc_interest.add(recv_chaincodeName);

        int timeout = 3 * 1000;
        CrossChainMessageReceipt sendTxResult =
                chaincodeInvoke(this.FABRIC_CC_FN_OUTER_RECV_MESSAGE, args, trans, cc_interest);
        return sendTxResult;
    }

    public CrossChainMessageReceipt deployContract(ChaincodeID chaincodeID, InputStream inputStream) {

        CrossChainMessageReceipt receipt = new CrossChainMessageReceipt();
        Collection<String> codenames = getDiscoveredChaincodeNames();
        for (String codename : codenames) {
            if (chaincodeID.getName() == codename) {
                logger.info("already find codename {}, ignore", codename);
                receipt.setConfirmed(true);
                receipt.setSuccessful(true);
                receipt.setErrorMsg("");
                return receipt;
            }
        }

        InstallProposalRequest installProposalRequest = hfClient.newInstallProposalRequest();
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        Collection<ProposalResponse> transactionPropResp;

        try  {
            installProposalRequest.setChaincodeInputStream(inputStream);
            installProposalRequest.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);
            installProposalRequest.setChaincodeID(chaincodeID);
            transactionPropResp =  hfClient.sendInstallProposal(installProposalRequest, channel.getPeers());
        } catch (Exception e) {
            logger.warn("can not find chaincode, path {}", chaincodeID.getPath(), e);
            receipt.setErrorMsg("can not find chaincode, path " + chaincodeID.getPath());
            receipt.setSuccessful(false);
            receipt.setConfirmed(false);
            return receipt;
        }

        for (ProposalResponse response : transactionPropResp) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                logger.info("FabricChaincode - Successful transaction proposal response Txid: {} from peer {}",
                        response.getTransactionID(),
                        response.getPeer().getName());
                successful.add(response);
            } else {
                failed.add(response);
                logger.info("failed code {}, reason: {}", response.getStatus(), response.getMessage());
            }
        }

        logger.info(
                "FabricChaincode - Received {} transaction proposal responses. Successful+verified: {} . Failed: {}",
                transactionPropResp.size(), successful.size(), failed.size());
        if (failed.size() > 0) {
            ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
            logger.warn("FabricChaincode - Not enough endorsers for invoke: " + failed.size() + " endorser error: " +
                    firstTransactionProposalResponse.getMessage() +
                    ". Was verified: " + firstTransactionProposalResponse.isVerified());
            CrossChainMessageReceipt ret = new CrossChainMessageReceipt();
            ret.setTxhash("");
            ret.setSuccessful(false);
            ret.setConfirmed(false);
            ret.setErrorMsg("Not enough endorsers for invoke");
            return ret;
        }

        logger.info("FabricChaincode - Successfully received transaction proposal responses.");

        String txid = "";
        try {
            final StringBuilder evenTransactionId = new StringBuilder();
            channel.sendTransaction(transactionPropResp).thenApply(transactionEvent -> {
                evenTransactionId.setLength(0);
                evenTransactionId.append(transactionEvent.getTransactionID());
                return null;
            }).exceptionally(e -> {
                if (e instanceof TransactionEventException) {
                    BlockEvent.TransactionEvent te = ((TransactionEventException) e).getTransactionEvent();
                    if (te != null) {
                        throw new RuntimeException(
                                format("Fabric invoke Transaction with txid %s failed. %s", te.getTransactionID(),
                                        e.getMessage()), e);
                    }
                }
                throw new RuntimeException(format("Fabric chaincode invoke failed with %s exception %s", e.getClass().getName(), e.getMessage()), e);
            }).get(3, TimeUnit.SECONDS);
            txid = evenTransactionId.toString();
        } catch (Exception e){
            logger.info("failed occur, {}", e.toString());
        }

        // String txid = ((LinkedList<ProposalResponse>) successful).getFirst().getTransactionID();
        logger.info("FabricChaincode - send tx success, txid: {}", txid);
        receipt.setTxhash(txid);
        receipt.setSuccessful(true);
        receipt.setConfirmed(false);
        receipt.setErrorMsg("");
        return receipt;
    }

    public CrossChainMessageReceipt queryTransactionInfo(String txid) {
        CrossChainMessageReceipt crossChainMessageReceipt = new CrossChainMessageReceipt();
        crossChainMessageReceipt.setTxhash(txid);
        try {
            TransactionInfo transactionInfo = channel.queryTransactionByID(txid);
            FabricTransaction.TxValidationCode retCode =  transactionInfo.getValidationCode();
            if ( retCode != FabricTransaction.TxValidationCode.VALID)  {
                logger.warn("transaction {} is invalid, error code {}", txid, retCode.getNumber());
                crossChainMessageReceipt.setConfirmed(false);
                crossChainMessageReceipt.setSuccessful(false);
                crossChainMessageReceipt.setErrorMsg("invalid transaction, error code" + retCode.getNumber());
            } else {
                logger.info("transaction {} is valid", txid);
                crossChainMessageReceipt.setConfirmed(true);
                crossChainMessageReceipt.setSuccessful(true);
                crossChainMessageReceipt.setErrorMsg("");
            }
        } catch (Exception e) {
            crossChainMessageReceipt.setConfirmed(false);
            crossChainMessageReceipt.setSuccessful(false);
            crossChainMessageReceipt.setErrorMsg("query txid unknown error " + e);
            logger.warn("transaction is invalid", e);
        }
        return crossChainMessageReceipt;
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

}
