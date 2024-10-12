package com.alipay.antchain.bridge.plugintestrunner.testcase;

import com.alipay.antchain.bridge.plugintestrunner.chainmanager.IChainManager;
import com.alipay.antchain.bridge.plugintestrunner.chainmanager.chainmaker.ChainMakerChainManager;
import com.alipay.antchain.bridge.plugintestrunner.chainmanager.eos.EosChainManager;
import com.alipay.antchain.bridge.plugintestrunner.chainmanager.eth.EthChainManager;
import com.alipay.antchain.bridge.plugintestrunner.chainmanager.fabric.FabricChainManager;
import com.alipay.antchain.bridge.plugintestrunner.chainmanager.fiscobcos.FiscoBcosChainManager;
import com.alipay.antchain.bridge.plugintestrunner.chainmanager.hyperchain.HyperchainChainManager;
import com.alipay.antchain.bridge.plugintestrunner.config.ChainProduct;
import com.alipay.antchain.bridge.plugintestrunner.exception.TestCaseException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.alipay.antchain.bridge.plugintestrunner.exception.TestCaseException.*;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public abstract class TestCaseChainConf {

    public abstract boolean isValid();

    public IChainManager toChainManager() throws Exception {
        ChainProduct cp = ChainProduct.fromValue(product);
        switch (cp) {
            case ETH:
                return new EthChainManager(((TestCaseEthConf) this).getHttpUrl(), ((TestCaseEthConf) this).getPrivateKeyFile(), ((TestCaseEthConf) this).getGasPrice(), ((TestCaseEthConf) this).getGasLimit());
            case EOS:
                return new EosChainManager(((TestCaseEosConf) this).getHttpUrl(), ((TestCaseEosConf) this).getPrivateKeyFile());
            case BCOS:
                return new FiscoBcosChainManager(((TestCaseBcosConf) this).getConfDir());
            case FABRIC:
                return new FabricChainManager(((TestCaseFabricConf) this).getConfFile());
            case CHAINMAKER:
                return new ChainMakerChainManager(((TestCaseChainMakerConf) this).getConfFile());
            case HYPERCHAIN:
                return new HyperchainChainManager(((TestCaseHyperChainConf) this).getHttpUrl());
            default:
                throw new TestCaseChainConfToChainManagerException("Unknown product type: " + product);
        }
    }

    // 添加一个静态方法来解析 JSON
    public static TestCaseChainConf fromJson(String jsonString, String product) throws JsonProcessingException, TestCaseException {
        ObjectMapper mapper = new ObjectMapper();
        ChainProduct cp = ChainProduct.fromValue(product);
        switch (cp) {
            case ETH:
                return mapper.readValue(jsonString, TestCaseEthConf.class);
            case EOS:
                return mapper.readValue(jsonString, TestCaseEosConf.class);
            case BCOS:
                return mapper.readValue(jsonString, TestCaseBcosConf.class);
            case FABRIC:
                return mapper.readValue(jsonString, TestCaseFabricConf.class);
            case CHAINMAKER:
                return mapper.readValue(jsonString, TestCaseChainMakerConf.class);
            case HYPERCHAIN:
                return mapper.readValue(jsonString, TestCaseHyperChainConf.class);
            default:
                throw new TestCaseChainConfToClassException("Unknown product type: " + product);
        }
    }

    // 基类的 product 字段
    @JsonIgnore
    private String product;

    protected void setProduct(String product) {
        this.product = product;
    }

    // 将内部类声明为静态类，并添加 getter 和 setter 方法
    @Getter
    @Setter
    public static class TestCaseEthConf extends TestCaseChainConf {
        private String httpUrl;
        private String privateKeyFile;
        private String gasPrice;
        private String gasLimit;

        public TestCaseEthConf() {
            setProduct("TestCaseEthConf");
        }

        @Override
        public boolean isValid() {
            return httpUrl != null && privateKeyFile != null && gasPrice != null && gasLimit != null;
        }
    }

    @Setter
    @Getter
    public static class TestCaseEosConf extends TestCaseChainConf {
        private String httpUrl;
        private String privateKeyFile;

        public TestCaseEosConf() {
            setProduct("TestCaseEosConf");
        }

        @Override
        public boolean isValid() {
            return httpUrl != null && privateKeyFile != null;
        }
    }

    @Setter
    @Getter
    public static class TestCaseBcosConf extends TestCaseChainConf {
        private String confDir;

        public TestCaseBcosConf() {
            setProduct("TestCaseBcosConf");
        }

        @Override
        public boolean isValid() {
            return confDir != null;
        }
    }

    @Setter
    @Getter
    public static class TestCaseFabricConf extends TestCaseChainConf {
        private String confFile;

        public TestCaseFabricConf() {
            setProduct("TestCaseFabricConf");
        }

        @Override
        public boolean isValid() {
            return confFile != null;
        }

    }

    @Setter
    @Getter
    public static class TestCaseChainMakerConf extends TestCaseChainConf {
        private String confFile;

        public TestCaseChainMakerConf() {
            setProduct("TestCaseChainMakerConf");
        }

        @Override
        public boolean isValid() {
            return confFile != null;
        }
    }

    @Setter
    @Getter
    public static class TestCaseHyperChainConf extends TestCaseChainConf {
        private String httpUrl;

        public TestCaseHyperChainConf() {
            setProduct("TestCaseHyperChainConf");
        }

        @Override
        public boolean isValid() {
            return httpUrl != null;
        }
    }
}