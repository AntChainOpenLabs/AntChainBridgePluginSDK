package com.alipay.antchain.bridge.plugins.spi.bbc.core.write;

/**
 * Through {@code IVerifierWriter}, you can write data
 * to the storage of the VerifierContract.
 *
 * @author zouxyan
 */
public interface IVerifierWriter {

    /**
     * Add raw <b>Third-Party Blockchain Trust Anchor</b>
     * abbreviated as {@code TPBTA} of a domain.
     *
     * <p>
     *     TPBTA is the trust root for contract ptc to verify the
     *     endorsements from PTC for ACB data like the cross-chain
     *     message. Every blockchain network registered into ACB
     *     needs to apply a domain certificate to own a domain which
     *     is the only representation of the blockchain in the ACB.
     * </p>
     *
     * <p>
     *     From the actual situation, developers need to send a transaction
     *     through your blockchain client to transport the {@code TPBTA}
     *     into the contract PTC.
     * </p>
     *
     * @param domain   domain is the only representation of the blockchain.
     * @param rawTPBTA {@code TPBTA} deserialized in TLV.
     */
    void addTPBTA(String domain, byte[] rawTPBTA);

    /**
     * Approve the protocol contract like {@code AuthMessage} contract to
     * call the {VerifierContract}.
     *
     * <p>
     *     Protocols for cross-chain communication needs to verify the
     *     cross-chain message whether it valid or not by calling the PTC
     *     contract which provides the interface to verify the {@code PTCProof}.
     *     For example, approve {@code AuthMessage} contract on PTC contract
     *     before starting commit cross-chain messages .
     * </p>
     *
     * @param protocolAddress contract address of protocol.
     */
    void approveProtocol(String protocolAddress);

    /**
     * Remove the approval of the protocol contract.
     *
     * @param protocolAddress protocol address to remove
     */
    void disapproveProtocol(String protocolAddress);

    /**
     * Set the owner for the PTC contract who can do actions like approvals.
     *
     * @param address owner address
     */
    void setOwner(String address);
}
