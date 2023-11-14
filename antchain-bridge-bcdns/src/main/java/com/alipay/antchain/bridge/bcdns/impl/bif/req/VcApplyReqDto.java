package org.bcdns.credential.dto.req;

import lombok.Data;

@Data
public class VcApplyReqDto {

    private byte[] content;

    private Integer credentialType;

    private String publicKey;

    private byte[] sign;
}

