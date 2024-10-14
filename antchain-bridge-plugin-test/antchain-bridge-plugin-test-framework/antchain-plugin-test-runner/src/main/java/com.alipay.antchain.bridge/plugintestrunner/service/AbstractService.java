package com.alipay.antchain.bridge.plugintestrunner.service;

import com.alipay.antchain.bridge.plugintestrunner.testcase.TestCase;
import com.alipay.antchain.bridge.plugintestrunner.util.PTRLogger;
import lombok.Getter;

import java.io.IOException;

@Getter
public abstract class AbstractService {
    protected final PTRLogger logger;

    public AbstractService(PTRLogger logger) {
        this.logger = logger;
    }

    public abstract void run(TestCase testCase) throws IOException;

    public abstract void close();
}

