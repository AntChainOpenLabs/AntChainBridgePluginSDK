package org.bcdns.credential.dto.req;

import lombok.Data;
import org.bif.common.validate.Validatable;

@Data
public class BlobReqDto {

    @Validatable(value = "txHash")
    private String txHash;

    @Validatable(value = "blob")
    private String blob;

}
