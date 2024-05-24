package com.alipay.antchain.bridge.plugins.fabric;

import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.plugins.lib.BBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;

import java.util.List;

@BBCService(products = "fabric", pluginId = "fabric_bbcservice")
public class FabricBBCService extends AbstractBBCService {
    // Fabric
    private Fabric14Client fabric14Client;
    private AbstractBBCContext fabricContext;

    @Override
    public void startup(AbstractBBCContext bbcContext) {
        getBBCLogger().info("[FabricBBCService] start up Fabric bbc service, context: {}",
                JSON.toJSONString(bbcContext));

        // start fabric client
        this.fabricContext = bbcContext;
        fabric14Client = new Fabric14Client(new String(this.fabricContext.getConfForBlockchainClient()), getBBCLogger());
        fabric14Client.start();
    }

    @Override
    public void shutdown() {
        getBBCLogger().info("[FabricBBCService] shutdown Fabric bbc service, context: {}",
                JSON.toJSONString(this.fabricContext));
        fabric14Client.shutdown();
    }

    @Override
    public AbstractBBCContext getContext() {
        return this.fabricContext;
    }

    @Override
    public CrossChainMessageReceipt readCrossChainMessageReceipt(String txid) {
        return fabric14Client.queryTransactionInfo(txid);
    }

    @Override
    public List<CrossChainMessage> readCrossChainMessagesByHeight(long l) {
        return fabric14Client.readCrossChainMessageReceipt(l);
    }

    @Override
    public Long queryLatestHeight() {
        return fabric14Client.getLastBlockHeight();
    }

    @Override
    public long querySDPMessageSeq(String senderDomain, String from, String receiverDomain, String to) {
        getBBCLogger().info("[FabricBBCService] query SDPMessageSeq sender domain {}, from {}, receiver domain {}, to {}",
                senderDomain, from, receiverDomain, to);
        return fabric14Client.querySDPMessageSeq(senderDomain, from, to);
    }

    @Override
    public void setupAuthMessageContract() {

    }

    @Override
    public void setupSDPMessageContract() {
        getBBCLogger().info("[FabricBBCService] setup SDPMessageContract is useless, ignore");
    }

    @Override
    public void setProtocol(String s, String s1) {
        // TODO, seems fabric does not have sdp contract
    }

    @Override
    public CrossChainMessageReceipt relayAuthMessage(byte[] bytes) {
        getBBCLogger().info("[FabricBBCService] relay AuthMessage");
        // TODO, what is serviceId
        return fabric14Client.recvPkgFromRelayer(bytes);
    }

    @Override
    public void setAmContract(String s) {
        // do not need set am contract in sdp contract, because fabric contract
        // do not have sdp contract
    }

    @Override
    public void setLocalDomain(String localDomain) {
        getBBCLogger().info("[FabricBBCService] set local domain {}", localDomain);
        fabric14Client.setLocalDomain(localDomain);
    }

}
