package org.bcdns.credential.dto.req;

import lombok.Data;
import org.bif.common.validate.Validatable;

@Data
public class VcCreateTemplateReqDto extends BlobReqDto{

    @Validatable(value = "模板名字")
    private String name;
    @Validatable(value = "行业分类Id")
    private String industryId;
    @Validatable(value = "模板所属分类Id")
    private String categoryId;
    @Validatable(value = "模板版本")
    private String version;
    private String remark;
    @Validatable(value = "模板元数据和审核数据")
    private String data;
    @Validatable(value = "面向用户类型")
    private String userType;
    @Validatable(value = "发证方bid")
    private String issuerBid;
    @Validatable(value = "模板bid")
    private String templateBid;

}
