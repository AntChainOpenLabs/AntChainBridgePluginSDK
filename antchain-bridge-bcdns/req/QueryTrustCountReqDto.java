package org.bcdns.credential.dto.req;

import lombok.Data;
import org.bcdns.credential.common.constant.Constants;

import java.util.ArrayList;
import java.util.List;

@Data
public class QueryTrustCountReqDto {
    private String superNodeBid;
    private List<String> templateBids;

    public List<String> getTemplateBids(){
        templateBids = new ArrayList<>();
        templateBids.add(Constants.COMPANY_TRUSTED_TEMPLATE_BID);
        templateBids.add(Constants.PERSON_TRUSTED_TEMPLATE_BID);
        templateBids.add(Constants.GOVERNMENT_TRUSTED_TEMPLATE_BID);
        return templateBids;
    }
}
