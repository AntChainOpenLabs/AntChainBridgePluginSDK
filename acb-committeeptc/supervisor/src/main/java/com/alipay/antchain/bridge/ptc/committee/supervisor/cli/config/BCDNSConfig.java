package com.alipay.antchain.bridge.ptc.committee.supervisor.cli.config;

import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.bcdns.service.BCDNSTypeEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Getter
@Setter
@Builder
public class BCDNSConfig {
    @JSONField(name = "bcdns_type")
    private BCDNSTypeEnum bcdnsType;

    @JSONField(name = "bcdns_path")
    private Path bcdnsPath;

    public BCDNSConfig() {};

    public BCDNSConfig(BCDNSTypeEnum bcdnsType, Path bcdnsClientFilePath) {
        this.setBcdnsType(bcdnsType);
        this.setBcdnsPath(bcdnsClientFilePath);
    };
}
