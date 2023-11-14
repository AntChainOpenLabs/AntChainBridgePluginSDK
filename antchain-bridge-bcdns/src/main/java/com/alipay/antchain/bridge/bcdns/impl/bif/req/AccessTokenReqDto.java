package org.bcdns.credential.dto.req;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

import static org.bcdns.credential.common.constant.MessageConstant.DESC_VALID_NULL;
import static org.bcdns.credential.common.constant.MessageConstant.DESC_VALID_STRING;
@Getter
@Setter
@Data
public class AccessTokenReqDto {
    @NotBlank(message = DESC_VALID_NULL)
    @Length(min = 1, max = 100, message = DESC_VALID_STRING)
    private String apiKey;

    @NotBlank(message = DESC_VALID_NULL)
    @Length(min = 1, max = 255, message = DESC_VALID_STRING)
    private String apiSecret;

    @NotBlank(message = DESC_VALID_NULL)
    private String issuerId;
}
