package com.alipay.antchain.bridge.relayer.server.admin;

import java.util.HashMap;
import java.util.Map;

import com.alipay.antchain.bridge.relayer.server.admin.impl.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NamespaceManager {

    private final Map<String, Namespace> namespaces = new HashMap<>();

    @Autowired
    public NamespaceManager(
            BlockchainNamespace blockchainNamespace,
            BCDNSNamespace bcdnsNamespace,
            ServiceNamespace serviceNamespace,
            RelayerNamespace relayerNamespace,
            PtcNamespace ptcNamespace
    ) {
        namespaces.put("service", serviceNamespace);
        namespaces.put("blockchain", blockchainNamespace);
        namespaces.put("relayer", relayerNamespace);
        namespaces.put("bcdns", bcdnsNamespace);
        namespaces.put("ptc", ptcNamespace);
    }

    public Namespace get(String name) {
        return namespaces.get(name);
    }
}
