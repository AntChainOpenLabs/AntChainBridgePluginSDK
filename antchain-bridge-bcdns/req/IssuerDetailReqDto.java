package org.bcdns.credential.dto.req;

import cn.blockchain.common.validate.Validatable;
import lombok.Data;

@Data
public class IssuerDetailReqDto extends BlobReqDto{

    @Validatable(value = "发证方bid")
    private String issuerBid;
    private String bid;
}
