package com.alipay.antchain.bridge.plugins.spi.bbc;

import com.alipay.antchain.bridge.commons.core.ptc.TPProof;

public interface IValidator {

    boolean verifyPTCProof(TPProof proof);

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
     * Determine if there is a {@code TPBTA} for the domain
     * name in the contract.
     *
     * @param domain blockchain domain name
     * @return boolean yes or no
     */
    boolean hasTPBTA(String domain);

    /**
     * Get the raw {@code TPBTA} for the domain.
     *
     * @param domain blockchain domain name
     * @return {@link byte[]} raw {@code TPBTA}
     */
    byte[] getTPBTA(String domain);

    /**
     *
     */
    boolean ifBlockchainProductSupported(String product);

    void setVerifyContract(String contractAddress);

    String getVerifyContract();
}
