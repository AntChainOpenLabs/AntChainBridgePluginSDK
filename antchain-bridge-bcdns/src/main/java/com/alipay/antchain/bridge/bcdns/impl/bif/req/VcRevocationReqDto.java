package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class VcRevocationReqDto extends BlobReqDto{

    private String auditBid;
    private String auditNodeAddress;
    private String credentialBid;
    private String remark;

}
