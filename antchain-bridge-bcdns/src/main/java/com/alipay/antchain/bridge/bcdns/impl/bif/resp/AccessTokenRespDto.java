package org.bcdns.credential.dto.resp;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AccessTokenRespDto {
    private String accessToken;
    private Integer expireIn;
}
