package com.alipay.antchain.bridge.plugins.mychain.sdp;

import com.alipay.antchain.bridge.plugins.mychain.common.BizContractTypeEnum;
import com.alipay.antchain.bridge.plugins.mychain.sdk.Mychain010Client;
import com.alipay.mychain.sdk.api.utils.Utils;
import com.alipay.mychain.sdk.domain.account.Identity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractDemoContract {

    private String domain;

    private String contractName;

    private Mychain010Client mychain010Client;

    private String sdpContractName;

//    private FabricHelper fabricHelper;


    public Identity getContractId() {
        return Utils.getIdentityByName(contractName);
    }

    public abstract BizContractTypeEnum getBizContractType();
}
