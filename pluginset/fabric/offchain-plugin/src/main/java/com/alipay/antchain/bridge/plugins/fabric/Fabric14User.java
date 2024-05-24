package com.alipay.antchain.bridge.plugins.fabric;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Set;

public class Fabric14User implements User {
    private static final Logger LOGGER = LoggerFactory.getLogger(Fabric14User.class);

    private String name;
    private Set<String> roles;
    private String account;
    private String affiliation;
    //private String organization;
    private String mspId;

    Enrollment enrollment = null; //need access in test env.

    public Fabric14User(String name) {
        this.name = name;
        //this.organization = org;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {

        this.roles = roles;
        //saveState();
    }

    @Override
    public String getAccount() {
        return account;
    }

    /**
     * Set the account.
     *
     * @param account The account.
     */
    public void setAccount(String account) {

        this.account = account;
        //saveState();
    }

    @Override
    public String getAffiliation() {
        return affiliation;
    }

    /**
     * Set the affiliation.
     *
     * @param affiliation the affiliation.
     */
    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
        //saveState();
    }

    @Override
    public Enrollment getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }

    public void setEnrollment(String privateKeyStr, String certificate) {

        PrivateKey privateKey = null;
        try {
            privateKey = getPrivateKeyFromBytes(privateKeyStr.getBytes());
        } catch (Exception e) {
            LOGGER.error("set enrollment, parse private key failed: ", e);
        }
        this.enrollment = new UserEnrollement(privateKey, certificate);
        //saveState();

    }

    @Override
    public String getMspId() {
        return this.mspId;
    }

    public void setMspId(String mspID) {
        this.mspId = mspID;
        //saveState();

    }

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    static PrivateKey getPrivateKeyFromBytes(byte[] data) throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        final Reader pemReader = new StringReader(new String(data));

        PrivateKeyInfo pemPair;
        try (PEMParser pemParser = new PEMParser(pemReader)) {
            pemPair = (PrivateKeyInfo) pemParser.readObject();
            PrivateKey privateKey = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getPrivateKey(pemPair);
            return privateKey;
        } catch (Exception e) {
           throw  e;
        }
    }

    static final class UserEnrollement implements Enrollment, Serializable {

        private static final long serialVersionUID = -2784835212445309006L;
        private final PrivateKey privateKey;
        private final String certificate;

        UserEnrollement(PrivateKey privateKey, String certificate) {

            this.certificate = certificate;

            this.privateKey = privateKey;
        }

        @Override
        public PrivateKey getKey() {

            return privateKey;
        }

        @Override
        public String getCert() {
            return certificate;
        }

    }
}
