package com.ali.antchain.Test;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetAmContractAndLocalDomain {

    private static final Logger log = LoggerFactory.getLogger(SetAmContractAndLocalDomain.class);
    AbstractBBCService service;
    String product;

    public SetAmContractAndLocalDomain(AbstractBBCService service) {
        this.service = service;
    }
    public static void run(AbstractBBCContext context, AbstractBBCService service, String product) throws Exception {
        if(product.equals("eth")){

        }
    }

    public void setamcontractandlocaldomain(AbstractBBCContext context) throws Exception {

    }
}
