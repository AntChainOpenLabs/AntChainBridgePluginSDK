package com.alipay.antchain.bridge.relayer.facade.admin.utils;

public class FacadeException extends RuntimeException {
    public FacadeException(String msg) {
        super(msg);
    }

    public FacadeException(String msg, Throwable t) {
        super(msg, t);
    }
}
