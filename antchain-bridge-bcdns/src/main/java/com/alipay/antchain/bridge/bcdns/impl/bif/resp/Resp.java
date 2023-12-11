package com.alipay.antchain.bridge.bcdns.impl.bif.resp;


import java.io.Serializable;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class Resp implements Serializable {
    private Integer errorCode;
    private String message;

    public void buildCommonField(Integer errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }
}
