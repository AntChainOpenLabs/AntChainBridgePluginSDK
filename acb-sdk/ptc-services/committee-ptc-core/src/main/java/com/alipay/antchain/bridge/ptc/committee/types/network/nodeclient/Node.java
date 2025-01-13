package com.alipay.antchain.bridge.ptc.committee.types.network.nodeclient;

import java.util.Set;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.alipay.antchain.bridge.ptc.committee.config.CommitteePtcConfig;
import com.alipay.antchain.bridge.ptc.committee.types.network.EndpointInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Node {

    public enum NodeState {
        AVAILABLE,
        UNAVAILABLE
    }

    public Node(EndpointInfo endpointInfo) {
        this.endpointInfo = endpointInfo;
    }

    private INodeClient nodeClient;

    private EndpointInfo endpointInfo;

    private NodeState nodeState = NodeState.UNAVAILABLE;

    private Set<String> productsSupported = new ConcurrentHashSet<>();

    public void connect(CommitteePtcConfig config) {
        nodeClient = endpointInfo.getNodeClient(config);
        nodeState = NodeState.AVAILABLE;
    }

    public boolean isAvailable() {
        return nodeState == NodeState.AVAILABLE;
    }
}
