package com.alipay.antchain.bridge.plugintestrunner.config;

import lombok.Getter;

@Getter
public enum ChainProduct {
    TESTCHAIN("testchain"),
    ETH("simple-ethereum"),
    CHAINMAKER("chainmaker"),
    FABRIC("fabric"),
    BCOS("fiscobcos"),
    EOS("eos"),
    HYPERCHAIN("hyperchain2");

    private final String value;

    ChainProduct(String value) {
        this.value = value;
    }

    // 静态方法，将字符串转换为 ChainType 枚举
    public static ChainProduct fromValue(String value) {
        for (ChainProduct type : ChainProduct.values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ChainProduct value: " + value);
    }

    public static boolean isInvalid(String value) {
        for (ChainProduct type : ChainProduct.values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return false;
            }
        }
        return true;
    }
}