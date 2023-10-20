package org.bcdns.credential.dto.req;

import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

import static org.bcdns.credential.common.constant.MessageConstant.DESC_VALID_NULL;
import static org.bcdns.credential.common.constant.MessageConstant.DESC_VALID_STRING;

public class VcApplyReqDto {


    @NotBlank(message = DESC_VALID_NULL)
    private String content;

    @NotBlank(message = DESC_VALID_NULL)
    @Length(min = 1,max = 64,message = DESC_VALID_STRING)
    private String templateId;

    @NotBlank(message = DESC_VALID_NULL)
    @Length(min = 1,max = 64,message = DESC_VALID_STRING)
    private String bid;

    private String publicKey;

    private int hold;

    public int getHold() {
        return hold;
    }

    public void setHold(int hold) {
        this.hold = hold;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }
}
