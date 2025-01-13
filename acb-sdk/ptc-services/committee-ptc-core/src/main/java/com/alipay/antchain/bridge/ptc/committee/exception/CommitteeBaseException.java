package com.alipay.antchain.bridge.ptc.committee.exception;

import cn.hutool.core.util.StrUtil;

public class CommitteeBaseException extends RuntimeException {

    public CommitteeBaseException(String message) {
        super(message);
    }

    public CommitteeBaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommitteeBaseException(Throwable cause) {
        super(cause);
    }

    public CommitteeBaseException(Throwable cause, String formatStr, Object... args) {
        super(StrUtil.format(formatStr, args), cause);
    }

    public CommitteeBaseException(String formatStr, Object... args) {
        super(StrUtil.format(formatStr, args));
    }
}
