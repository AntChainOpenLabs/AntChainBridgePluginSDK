package org.bcdns.credential.dto.resp;


import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import org.bcdns.credential.enums.ExceptionEnum;
import org.bcdns.credential.exception.APIException;

import java.io.Serializable;

public class Resp implements Serializable {
    private Integer errorCode;
    private String message;

    public void buildCommonField(Integer errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public void buildAPIExceptionField(APIException e) {
        buildCommonField(e.getErrorCode(), e.getErrorMessage());
    }
    public void buildAntChainBridgeCommonsExceptionField(AntChainBridgeCommonsException e) {
        buildCommonField(Integer.parseInt(e.getCode()), e.getMessage());
    }

    public void buildSuccessField() {
        buildExceptionEnumField(ExceptionEnum.SUCCESS);
    }

    public void buildSysExceptionField() {
        buildExceptionEnumField(ExceptionEnum.SYS_ERROR);
    }

    public void buildExceptionEnumField(ExceptionEnum e) {
        buildCommonField(e.getErrorCode(), e.getMessage());
    }


    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
