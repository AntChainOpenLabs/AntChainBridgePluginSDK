package com.alipay.antchain.bridge.relayer.facade.admin.types;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BlockchainId {

    @JSONField(name = "product")
    private String product;

    @JSONField(name = "blockchain_id")
    private String blockchainId;
}
