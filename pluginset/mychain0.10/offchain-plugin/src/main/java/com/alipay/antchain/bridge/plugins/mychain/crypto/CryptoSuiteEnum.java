package com.alipay.antchain.bridge.plugins.mychain.crypto;

public enum CryptoSuiteEnum {
    CRYPTO_SUITE_UNKNOW(0, "unknow"),

    /**
     * default
     * - sign：secp256k1
     * - hash：sha256
     */
    CRYPTO_SUITE_DEFAULT(1, "default"),

    /**
     * sm
     * - sign：sm2
     * - hash：sm3
     */
    CRYPTO_SUITE_SM(2, "sm");

    private final int value;

    private final String name;

    private CryptoSuiteEnum(int v, String n){
        value = v;
        name = n;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    /**
     * Get enum from int
     */
    public static CryptoSuiteEnum fromValue(int value) {
        for (CryptoSuiteEnum v : CryptoSuiteEnum.values()) {
            if(v.getValue() == value) {
                return v;
            }
        }
        return null;
    }

    /**
     * Get enum from name(string)
     */
    public static CryptoSuiteEnum fromName(String name) {
        for (CryptoSuiteEnum v : CryptoSuiteEnum.values()) {
            if(v.getName().equals(name)) {
                return v;
            }
        }
        return null;
    }
}
