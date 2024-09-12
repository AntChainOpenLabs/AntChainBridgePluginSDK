package com.ali.antchain.Test;

import com.ali.antchain.abi.AuthMsg;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import org.junit.Assert;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.DefaultGasProvider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;

public abstract class Tester {

    AbstractBBCService service;

    public Tester(AbstractBBCService service) {
        this.service = service;
    }

    public abstract void getProtocol() throws Exception;

    public abstract void checkAm();
}
