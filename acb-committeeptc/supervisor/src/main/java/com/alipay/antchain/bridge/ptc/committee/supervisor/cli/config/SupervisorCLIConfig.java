package com.alipay.antchain.bridge.ptc.committee.supervisor.cli.config;

import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SupervisorCLIConfig {
    @JSONField(name = "committee_id")
    private String committeeId;

    @JSONField(name = "private_key")
    private String privateKey;

    @JSONField(name = "public_key")
    private String publicKey;

    @JSONField(name = "ptc_certificate")
    private String ptcCertificate;

    @JSONField(name = "sign_algo")
    private SignAlgoEnum signAlgo;

    @JSONField(name = "bcdns_config")
    private Map<String, BCDNSConfig> bcdnsConfig;
}
