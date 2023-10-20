package com.alipay.antchain.bridge.bcdns.impl.bif.req;

public class TxQueryReqDto {

    private String hash;
    private String chainCode;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getChainCode() {
        return chainCode;
    }

    public void setChainCode(String chainCode) {
        this.chainCode = chainCode;
    }
}
