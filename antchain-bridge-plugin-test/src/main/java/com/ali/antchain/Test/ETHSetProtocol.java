//package com.ali.antchain.Test;
//
//import com.ali.antchain.abi.AuthMsg;
//import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
//import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
//import org.eclipse.core.internal.runtime.Product;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.web3j.tx.gas.DefaultGasProvider;
//import java.math.BigInteger;
//
//public class ETHSetProtocol extends SetProtocolTest {
//    private static final Logger log = LoggerFactory.getLogger(ETHSetProtocol.class);
//
//    public ETHSetProtocol(AbstractBBCService service,String product) {
//        super(service,product);
//    }
//    public static void run(AbstractBBCContext context, AbstractBBCService service) throws Exception {
//        ETHSetProtocol setProtocol = new ETHSetProtocol(service, );
//        setProtocol.setprotocol_success(context);
//    }
//
//    @Override
//    public void setprotocol_success(AbstractBBCContext context) throws Exception {
//
//        System.out.println("eth setprotocol test ...");
////        EthereumBBCService service = new EthereumBBCService();
//        // start up
//        service.startup(context);
//
//        // set up am
//        service.setupAuthMessageContract();
//
//        // set up sdp
//        service.setupSDPMessageContract();
//
//        // get context
//        AbstractBBCContext ctx = service.getContext();
//
////        service.setProtocol(ctx.getSdpContract().getContractAddress(),"0");
////        String addr = AuthMsg.load(service.getContext().getAuthMessageContract().getContractAddress(),
////                service.getWeb3j(),
////                service.getCredentials(),
////                new DefaultGasProvider()).getProtocol(BigInteger.ZERO).send();
////        log.info("protocol: {}", addr);
//
//        // check am status
//        ctx = service.getContext();
//        log.info("am contract status: {}",ctx.getAuthMessageContract().getStatus());
//    }
//}
