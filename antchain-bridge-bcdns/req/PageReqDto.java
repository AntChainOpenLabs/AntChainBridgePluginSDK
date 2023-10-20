package org.bcdns.credential.dto.req;

import lombok.Data;

/**
 * @author skye
 * @version 1.0
 * @description:
 * @date 2021/5/13 9:57
 */
@Data
public class PageReqDto {

    private int pageStart = 1;
    private int pageSize = 10;
    private int startNum;
}
