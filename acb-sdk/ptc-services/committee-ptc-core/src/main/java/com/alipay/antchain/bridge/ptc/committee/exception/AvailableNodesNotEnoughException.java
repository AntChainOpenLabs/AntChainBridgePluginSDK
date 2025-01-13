package com.alipay.antchain.bridge.ptc.committee.exception;

public class AvailableNodesNotEnoughException extends CommitteeBaseException {

    public AvailableNodesNotEnoughException(String message) {
        super(message);
    }

    public AvailableNodesNotEnoughException() {
        super("Active nodes not enough For service requirement, at least 2/3 nodes is active");
    }
}
