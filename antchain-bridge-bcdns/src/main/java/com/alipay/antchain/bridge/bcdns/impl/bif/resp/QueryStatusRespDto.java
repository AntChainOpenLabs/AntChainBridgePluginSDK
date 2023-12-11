package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import lombok.Data;

@Data
public class QueryStatusRespDto {
    private Integer status;
    private String credentialId;
    private ObjectIdentity userId;
}
