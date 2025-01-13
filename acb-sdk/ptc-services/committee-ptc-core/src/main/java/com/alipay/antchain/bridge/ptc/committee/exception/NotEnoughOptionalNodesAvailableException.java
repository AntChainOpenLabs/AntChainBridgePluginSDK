package com.alipay.antchain.bridge.ptc.committee.exception;

public class NotEnoughOptionalNodesAvailableException extends CommitteeBaseException {
    public NotEnoughOptionalNodesAvailableException() {
        super("Not enough optional nodes available to meet the policy");
    }
}
