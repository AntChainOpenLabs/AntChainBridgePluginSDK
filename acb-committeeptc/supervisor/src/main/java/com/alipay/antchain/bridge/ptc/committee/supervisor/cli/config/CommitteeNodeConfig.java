package com.alipay.antchain.bridge.ptc.committee.supervisor.cli.config;

import java.util.Map;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CommitteeNodeConfig {
    @JSONField(name = "node_id")
    private String nodeId;

    @JSONField(name = "tls_cert")
    private String tlsCert;

    @JSONField(name = "keys")
    private Map<String, String> keys;

    @JSONField(name = "endpoint_url")
    private String endPointUrl;
}
