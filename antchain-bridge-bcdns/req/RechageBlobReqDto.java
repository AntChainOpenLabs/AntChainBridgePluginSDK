package org.bcdns.credential.dto.req;

import lombok.Data;

@Data
public class RechageBlobReqDto {
    private String chainCode;
    private String address;
    private String amount;
}