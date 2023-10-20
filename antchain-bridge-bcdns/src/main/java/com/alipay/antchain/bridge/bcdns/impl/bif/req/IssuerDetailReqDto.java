package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import lombok.Data;

@Data
public class IssuerDetailReqDto extends BlobReqDto{

    private String issuerBid;
    private String bid;
}
