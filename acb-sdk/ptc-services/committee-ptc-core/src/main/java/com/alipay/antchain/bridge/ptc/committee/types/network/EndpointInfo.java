package com.alipay.antchain.bridge.ptc.committee.types.network;

import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.ptc.committee.config.CommitteePtcConfig;
import com.alipay.antchain.bridge.ptc.committee.types.network.nodeclient.GrpcNodeClient;
import com.alipay.antchain.bridge.ptc.committee.types.network.nodeclient.INodeClient;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

@Getter
@Setter
public class EndpointInfo {

    @JSONField(name = "node_id")
    private String nodeId;

    @JSONField(name = "endpoint", serializeUsing = EndpointAddressSerializer.class, deserializeUsing = EndpointAddressDeserializer.class)
    private EndpointAddress endpoint;

    @JSONField(name = "tls_cert")
    private String tlsCert;

    @SneakyThrows
    @JSONField(serialize = false, deserialize = false)
    public X509Certificate getX509TlsCert() {
        if (StrUtil.isEmpty(tlsCert)) {
            return null;
        }
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        String temp = tlsCert;
        if (temp.contains("%20")) {
            temp = URLDecoder.decode(temp, "UTF-8");
        }
        return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(temp.getBytes()));
    }

    @JSONField(serialize = false, deserialize = false)
    public INodeClient getNodeClient(CommitteePtcConfig config) {
        switch (endpoint.getProtocolHeader()) {
            case GRPCS:
                return new GrpcNodeClient(
                        this,
                        config.getTlsClientPemPkcs8Key(),
                        config.getTlsClientPemCert()
                );
            case GRPC:
            default:
                throw new RuntimeException("Protocol not supported: " + endpoint.getProtocolHeader().getHeader());
        }
    }
}
