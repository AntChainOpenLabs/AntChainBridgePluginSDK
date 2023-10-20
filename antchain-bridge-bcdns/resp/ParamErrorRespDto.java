package org.bcdns.credential.dto.resp;

import lombok.Data;

import java.util.List;

@Data
public class ParamErrorRespDto {
    List<String> errorList;
}