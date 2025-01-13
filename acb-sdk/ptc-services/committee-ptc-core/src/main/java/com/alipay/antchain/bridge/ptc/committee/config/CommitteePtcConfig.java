package com.alipay.antchain.bridge.ptc.committee.config;

import java.io.IOException;
import java.lang.reflect.Type;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.ptc.committee.types.network.CommitteeNetworkInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommitteePtcConfig {

    public static class PtcCertSerializer implements ObjectSerializer {
        @Override
        public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
            serializer.write(CrossChainCertificateUtil.formatCrossChainCertificateToPem((AbstractCrossChainCertificate) object));
        }
    }

    public static class PtcCertDeserializer implements ObjectDeserializer {
        @Override
        public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
            String value = parser.parseObject(String.class);
            return (T) CrossChainCertificateUtil.readCrossChainCertificateFromPem(value.getBytes());
        }

        @Override
        public int getFastMatchToken() {
            return 0;
        }
    }

    public static CommitteePtcConfig parseFrom(byte[] rawConfig) {
        return JSON.parseObject(rawConfig, CommitteePtcConfig.class);
    }

    @JSONField(name = "tls_client_pem_pkcs8_key")
    private String tlsClientPemPkcs8Key;

    @JSONField(name = "tls_client_pem_cert")
    private String tlsClientPemCert;

    @JSONField(name = "request_threads_pool_core_size")
    private int requestThreadsPoolCoreSize = 4;

    @JSONField(name = "request_threads_pool_max_size")
    private int requestThreadsPoolMaxSize = 4;

    @JSONField(name = "heartbeat_interval")
    private int heartbeatInterval = 5000;

    @JSONField(name = "ptc_certificate", serializeUsing = PtcCertSerializer.class, deserializeUsing = PtcCertDeserializer.class)
    private AbstractCrossChainCertificate ptcCertificate;

    @JSONField(name = "network")
    private CommitteeNetworkInfo committeeNetworkInfo;

    public byte[] encode() {
        return JSON.toJSONBytes(this);
    }
}
