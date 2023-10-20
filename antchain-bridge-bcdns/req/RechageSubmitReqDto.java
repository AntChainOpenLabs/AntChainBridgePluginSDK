package org.bcdns.credential.dto.req;

import lombok.Data;

@Data
public class RechageSubmitReqDto {
    private String chainCode;
    private String signBlob;
    private String publicKey;
    private String blob;
    private String txHash;
}
