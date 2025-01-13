package com.alipay.antchain.bridge.relayer.core.types.pluginserver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.pluginserver.service.*;
import com.alipay.antchain.bridge.relayer.commons.model.PluginServerInfo;
import lombok.Synchronized;

public class GRpcPluginServerClient implements IPluginServerClient {

    private final String psId;

    private CrossChainServiceGrpc.CrossChainServiceBlockingStub serviceStub;

    private final AtomicInteger errorCount = new AtomicInteger(0);

    private final int errorLimitForHeartbeat;

    public GRpcPluginServerClient(
            String psId,
            CrossChainServiceGrpc.CrossChainServiceBlockingStub serviceStub,
            int errorLimitForHeartbeat
    ) {
        this.psId = psId;
        this.serviceStub = serviceStub;
        this.errorLimitForHeartbeat = errorLimitForHeartbeat;
    }

    @Override
    @Synchronized
    public PluginServerInfo heartbeat() {
        Response response = this.serviceStub.heartbeat(Empty.getDefaultInstance());
        if (
                (ObjectUtil.isNull(response) || response.getCode() != 0)
                        && this.errorCount.getAndAdd(1) > this.errorLimitForHeartbeat
        ) {
            throw new RuntimeException(String.format("heartbeat failed out of limit %s for plugin server %s: %s",
                    this.errorCount.get(), this.psId, response.getErrorMsg()));
        }

        if (!response.hasHeartbeatResp()) {
            throw new RuntimeException(String.format("heartbeat response not found for plugin server %s", this.psId));
        }
        this.errorCount.set(0);
        return new PluginServerInfo(
                response.getHeartbeatResp().getProductsList(),
                response.getHeartbeatResp().getDomainsList()
        );
    }

    @Override
    public Map<String, Boolean> ifProductSupport(List<String> products) {
        Response response = this.serviceStub.ifProductSupport(
                IfProductSupportRequest.newBuilder()
                        .addAllProducts(products)
                        .build()
        );
        if (response.getCode() != 0) {
            throw new RuntimeException(String.format("ifProductSupport request failed for plugin server %s: %s",
                    this.psId, response.getErrorMsg()));
        }
        return response.getIfProductSupportResp().getResultsMap();
    }

    @Override
    public Map<String, Boolean> ifDomainAlive(List<String> domains) {
        Response response = this.serviceStub.ifDomainAlive(
                IfDomainAliveRequest.newBuilder()
                        .addAllDomains(domains)
                        .build()
        );
        if (response.getCode() != 0) {
            throw new RuntimeException(String.format("ifDomainAlive request failed for plugin server %s: %s",
                    this.psId, response.getErrorMsg()));
        }
        return response.getIfDomainAliveResp().getResultsMap();
    }
}
