package org.bcdns.credential.dto.resp;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
@Data
@NoArgsConstructor
public class ApiKeyRespDto {
    private String apiKey;
    private String apiSecret;
    private ObjectIdentity issuerId;
}
