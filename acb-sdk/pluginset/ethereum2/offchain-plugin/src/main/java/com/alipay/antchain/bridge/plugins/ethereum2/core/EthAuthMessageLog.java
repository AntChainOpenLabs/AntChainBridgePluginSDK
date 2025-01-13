package com.alipay.antchain.bridge.plugins.ethereum2.core;

import com.alibaba.fastjson.JSON;
import lombok.*;
import org.web3j.protocol.core.methods.response.Log;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EthAuthMessageLog {

    public static EthAuthMessageLog decodeFromJson(String json) {
        return JSON.parseObject(json, EthAuthMessageLog.class);
    }

    private Integer logIndex;

    private Log sendAuthMessageLog;

    public String encodeToJson() {
        return JSON.toJSONString(this);
    }
}
