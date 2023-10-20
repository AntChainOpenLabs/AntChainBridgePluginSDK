package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

import java.util.List;

import lombok.Data;

@Data
public class ParamErrorRespDto {
    List<String> errorList;
}