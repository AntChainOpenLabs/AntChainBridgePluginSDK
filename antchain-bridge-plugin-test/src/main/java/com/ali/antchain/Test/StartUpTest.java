package com.ali.antchain.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;

public class StartUpTest {

    private static final Logger log = LoggerFactory.getLogger(StartUpTest.class);
    AbstractBBCService service;

    public StartUpTest(AbstractBBCService service) {
        this.service = service;
    }

    public static void run(AbstractBBCContext context, AbstractBBCService service){
        StartUpTest startUpTest = new StartUpTest(service);
        startUpTest.startuptest_success(context);
    }

    public void startuptest_success(AbstractBBCContext  context) {
        try {
            service.startup(context);
            // 使用日志框架记录信息
            log.info("Context: {}", service.getContext());
            log.info("AuthMessageContract: {}", service.getContext().getAuthMessageContract());
            log.info("SdpContract: {}", service.getContext().getSdpContract());
        } catch (Exception e) {
            // 异常处理
            log.error("Error during startup test", e);
        }
    }
}
