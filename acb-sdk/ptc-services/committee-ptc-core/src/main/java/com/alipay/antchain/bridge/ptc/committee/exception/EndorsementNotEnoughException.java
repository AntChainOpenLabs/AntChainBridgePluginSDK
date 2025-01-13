package com.alipay.antchain.bridge.ptc.committee.exception;

import cn.hutool.core.util.StrUtil;

public class EndorsementNotEnoughException extends CommitteeBaseException {

    public EndorsementNotEnoughException(String message) {
        super(message);
    }

    public EndorsementNotEnoughException(String message, Throwable cause) {
        super(message, cause);
    }

    public EndorsementNotEnoughException(Throwable cause) {
        super(cause);
    }

    public EndorsementNotEnoughException(Throwable cause, String formatStr, Object... args) {
        super(StrUtil.format(formatStr, args), cause);
    }

    public EndorsementNotEnoughException(String formatStr, Object... args) {
        super(StrUtil.format(formatStr, args));
    }
}
