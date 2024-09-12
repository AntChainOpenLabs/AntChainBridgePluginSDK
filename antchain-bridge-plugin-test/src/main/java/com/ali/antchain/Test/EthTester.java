package com.ali.antchain.Test;

import com.ali.antchain.abi.AuthMsg;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import org.junit.Assert;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.DefaultGasProvider;

import java.lang.reflect.Method;
import java.math.BigInteger;

public class EthTester extends Tester{

    public EthTester(AbstractBBCService service) {
        super(service);
    }

    @Override
    public void getProtocol() throws Exception {
        Class<?> aClass = service.getClass();
        Method getWeb3j = aClass.getDeclaredMethod("getWeb3j", Web3j.class);
        Method getCredentials = aClass.getDeclaredMethod("getCredentials", Credentials.class);
        getWeb3j.setAccessible(true);
        getCredentials.setAccessible(true);
        String addr = AuthMsg.load(service.getContext().getAuthMessageContract().getContractAddress(),
                (Web3j) getWeb3j.invoke(aClass),
                (Credentials) getCredentials.invoke(aClass),
                new DefaultGasProvider()).getProtocol(BigInteger.ZERO).send();
        System.out.println("=================");
        System.out.println("Eth tester get protocol test....");
    }

    @Override
    public void checkAm() {
        // check am status
        AbstractBBCContext ctx = service.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getAuthMessageContract().getStatus());
//        log.info("am contract status: {}",ctx.getAuthMessageContract().getStatus());

        System.out.println("=================");
        System.out.println("Eth tester check am test....");
    }


}
