package com.alipay.antchain.bridge.plugins.spi.ptc.core;

public interface ILedgerParser {

    /**
     * Parse raw cross-chain message from the given ledger data.
     *
     * <p>
     *     This {@code ledgerData} is supposed to be the ledger structure
     *     containing the message created by the cross-chain contracts like
     *     the {@code Receipt} or {@code Event}, etc. This method can deserialize
     *     the {@code ledgerData} into an instance and read the message out.
     * </p>
     *
     * @param ledgerData serialized ledger data
     * @return bytes raw message
     */
    byte[] parseMessageFromLedgerData(byte[] ledgerData);
}
