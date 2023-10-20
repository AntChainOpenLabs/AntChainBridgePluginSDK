package com.alipay.antchain.bridge.bcdns.impl.bif.req;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class QueryTrustCountReqDto {
    private String superNodeBid;
    private List<String> templateBids;

    public List<String> getTemplateBids(){
        templateBids = new ArrayList<>();
//        templateBids.add(Constants.COMPANY_TRUSTED_TEMPLATE_BID);
//        templateBids.add(Constants.PERSON_TRUSTED_TEMPLATE_BID);
//        templateBids.add(Constants.GOVERNMENT_TRUSTED_TEMPLATE_BID);
        return templateBids;
    }
}
