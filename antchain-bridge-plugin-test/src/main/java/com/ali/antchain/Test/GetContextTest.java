package com.ali.antchain.Test;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetContextTest {

    private static final Logger log = LoggerFactory.getLogger(GetContextTest.class);
    AbstractBBCService service;

    public GetContextTest(AbstractBBCService service) {
        this.service = service;
    }
    public static void run(AbstractBBCContext context, AbstractBBCService service){
        GetContextTest getctx = new GetContextTest(service);
        getctx.getcontext(context);
    }
    public void getcontext(AbstractBBCContext context){
        if (service == null) {
            throw new IllegalStateException("Service is not initialized.");
        }
        try {
            // 启动服务
            service.startup(context);
            // 获取上下文
            AbstractBBCContext ctx = service.getContext();
            // 检查上下文是否为空
            if (ctx != null) {
                log.info( "Context: {}", ctx);

                // 打印 AM 合约
                processAuthMessageContract(ctx);

                // 打印 SDP 合约
                processSDPContract(ctx);

            } else {
                log.warn( "Context is null.");
            }
        } catch (Exception e) {
            // 处理异常
            log.error("An error occurred: ", e);
        }
    }
    private void processAuthMessageContract(AbstractBBCContext ctx) {
        AuthMessageContract authMessageContract = ctx.getAuthMessageContract();
        if (authMessageContract != null) {
            log.info("Auth Message Contract: {}", authMessageContract);
        }
    }

    private void processSDPContract(AbstractBBCContext ctx) {
        SDPContract sdpContract = ctx.getSdpContract();
        if (sdpContract != null) {
            log.info("SDP Contract: {}", sdpContract);
        }
    }
}

//public class GetContextTest {
//
//    private static final Logger log = LoggerFactory.getLogger(GetContextTest.class);
//
//    private AbstractBBCService service;
//
//    public GetContextTest(AbstractBBCService service) {
//        this.service = service;
//    }
//
//    public static void run(AbstractBBCContext context){
//        getcontext(context);
//    }
//
//    public void getcontext(AbstractBBCContext context){
//        if (service == null) {
//            throw new IllegalStateException("Service is not initialized.");
//        }
//        try {
//            service.startup(context);
//            AbstractBBCContext ctx = service.getContext();
//            if (ctx != null) {
//                log.info("Context: {}", ctx);
//                processAuthMessageContract(ctx);
//                processSDPContract(ctx);
//            } else {
//                log.warn("Context is null.");
//            }
//        } catch (Exception e) {
//            log.error("An unexpected error occurred: ", e);
//        }
//    }
//
//
//}
