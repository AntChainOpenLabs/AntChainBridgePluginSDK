package com.alipay.antchain.bridge.plugins.spi.bbc.core.read;

/**
 * Through {@code IVerifierReader}, you can query the state of the PTCContract.
 *
 * @author zouxyan
 */
public interface IVerifierReader {

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
}
