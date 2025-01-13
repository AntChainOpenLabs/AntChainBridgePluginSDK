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

package com.alipay.antchain.bridge.relayer.core.types.network.ws.client;

import java.util.concurrent.ExecutorService;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.ws.BindingProvider;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerCredentialManager;
import com.alipay.antchain.bridge.relayer.core.types.network.BaseRelayerClient;
import com.alipay.antchain.bridge.relayer.core.types.network.request.RelayerRequest;
import com.alipay.antchain.bridge.relayer.core.types.network.response.RelayerResponse;
import com.alipay.antchain.bridge.relayer.core.types.network.ws.client.generated.WSRelayerServerAPImpl;

public class WSRelayerClient extends BaseRelayerClient {

    private static final String HOSTNAME_VERIFIER = "com.sun.xml.internal.ws.transport.https.client.hostname.verifier";

    private static final String SSL_SOCKET_FACTORY = "com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory";

    private WSRelayerServerAPImpl wsEndpointServer;

    private final ExecutorService workers;

    private final SSLSocketFactory sslSocketFactory;

    public WSRelayerClient(
            RelayerNodeInfo remoteNodeInfo,
            IRelayerCredentialManager relayerCredentialManager,
            String defaultNetworkId,
            ExecutorService workers,
            SSLSocketFactory sslSocketFactory
    ) {
        super(remoteNodeInfo, relayerCredentialManager, defaultNetworkId);
        this.workers = workers;
        this.sslSocketFactory = sslSocketFactory;
    }

    @Override
    public void startup() {

        HostnameVerifier hostnameVerifier = (s, sslSession) -> true;

        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);

        if (ObjectUtil.isEmpty(getRemoteNodeInfo().getEndpoints())) {
            throw new RuntimeException(
                    String.format(
                            "failed to start WSRelayerServerAPImplService: zero size endpoints for relayer (node_id: %s)",
                            getRemoteNodeInfo().getNodeId()
                    )
            );
        }

        boolean isRunningOnTLS = false;
        WSRelayerServerAPImplServiceWithHost serverService = null;
        for (int idx = 0; idx < getRemoteNodeInfo().getEndpoints().size(); ++idx) {
            try {
                String url = getRemoteNodeInfo().getEndpoints().get(idx);
                if (!url.startsWith("http")) {
                    url += "https://";
                }
                serverService = new WSRelayerServerAPImplServiceWithHost(url);
                isRunningOnTLS = url.startsWith("https");
                break;
            } catch (Exception e) {
                if (idx == getRemoteNodeInfo().getEndpoints().size() - 1) {
                    throw new RuntimeException("failed to start WSRelayerServerAPImplService. ", e);
                }
            }
        }
        if (ObjectUtil.isNull(serverService)) {
            throw new RuntimeException("null webservice client made for relayer " + getRemoteNodeInfo().getNodeId());
        }

        serverService.setExecutor(workers);
        wsEndpointServer = serverService.getWSRelayerServerAPImplPort();

        BindingProvider bindingProvider = (BindingProvider) wsEndpointServer;

        if (isRunningOnTLS) {
            bindingProvider.getRequestContext().put(HOSTNAME_VERIFIER, hostnameVerifier);
            bindingProvider.getRequestContext().put(SSL_SOCKET_FACTORY, sslSocketFactory);
            bindingProvider.getRequestContext().put(
                    BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                    serverService.getWSDLDocumentLocation().toString()
            );
        }
    }

    @Override
    public boolean shutdown() {
        return true;
    }

    @Override
    public RelayerResponse sendRequest(RelayerRequest relayerRequest) {
        return RelayerResponse.decode(
                Base64.decode(
                        wsEndpointServer.request(
                                Base64.encode(relayerRequest.encode())
                        )
                )
        );
    }
}
