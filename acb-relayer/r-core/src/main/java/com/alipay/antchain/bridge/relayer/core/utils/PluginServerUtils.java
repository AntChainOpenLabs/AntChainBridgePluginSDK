package com.alipay.antchain.bridge.relayer.core.utils;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import sun.security.util.DerValue;
import sun.security.util.HostnameChecker;
import sun.security.x509.X500Name;

public class PluginServerUtils {

    public static String getPluginServerCertX509CommonName(String x509Cert) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate c = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(x509Cert.getBytes()));
            DerValue derValue = HostnameChecker.getSubjectX500Name(c)
                    .findMostSpecificAttribute(X500Name.commonName_oid);
            return derValue.getAsString();
        } catch (Exception e) {
            throw new RuntimeException("failed to get common name from x509 cert of pluginserver", e);
        }
    }

    public static com.alipay.antchain.bridge.pluginserver.service.CrossChainMessageReceipt convertFromCommonReceipt(CrossChainMessageReceipt receipt) {
        return com.alipay.antchain.bridge.pluginserver.service.CrossChainMessageReceipt.newBuilder()
                .setTxhash(receipt.getTxhash())
                .setConfirmed(receipt.isConfirmed())
                .build();
    }

    public static CrossChainMessage convertFromGRpcCrossChainMessage(com.alipay.antchain.bridge.pluginserver.service.CrossChainMessage crossChainMessage) {
        return CrossChainMessage.createCrossChainMessage(
                CrossChainMessage.CrossChainMessageType.parseFromValue(crossChainMessage.getType().getNumber()),
                crossChainMessage.getProvableData().getHeight(),
                crossChainMessage.getProvableData().getTimestamp(),
                crossChainMessage.getProvableData().getBlockHash().toByteArray(),
                crossChainMessage.getMessage().toByteArray(),
                crossChainMessage.getProvableData().getLedgerData().toByteArray(),
                crossChainMessage.getProvableData().getProof().toByteArray(),
                crossChainMessage.getProvableData().getTxHash().toByteArray()
        );
    }

    public static CrossChainMessageReceipt convertFromGRpcCrossChainMessageReceipt(com.alipay.antchain.bridge.pluginserver.service.CrossChainMessageReceipt crossChainMessageReceipt) {
        CrossChainMessageReceipt receipt = new CrossChainMessageReceipt();
        receipt.setConfirmed(crossChainMessageReceipt.getConfirmed());
        receipt.setSuccessful(crossChainMessageReceipt.getSuccessful());
        receipt.setTxhash(crossChainMessageReceipt.getTxhash());
        receipt.setErrorMsg(crossChainMessageReceipt.getErrorMsg());
        receipt.setTxTimestamp(crossChainMessageReceipt.getTxTimestamp());
        receipt.setRawTx(crossChainMessageReceipt.getRawTx().toByteArray());

        return receipt;
    }
}
