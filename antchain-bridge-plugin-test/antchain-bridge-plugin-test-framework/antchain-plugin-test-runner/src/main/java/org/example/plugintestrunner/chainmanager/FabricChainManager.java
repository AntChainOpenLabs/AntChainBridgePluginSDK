package org.example.plugintestrunner.chainmanager;

import lombok.Setter;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.user.*;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class FabricChainManager extends IChainManager{

    HFClient hfClient;
    Channel channel;
    Fabric14User user;


    public FabricChainManager(String private_key_file, String cert_file, String peer_tls_cert_file, String orderer_tls_cert_file) throws InvalidArgumentException, CryptoException, ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, TransactionException, ProposalException {
        hfClient = HFClient.createNewInstance();
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        user = new Fabric14User("User1@org1.example.com");
        user.setMspId("Org1MSP");
        user.setEnrollment(readFile(private_key_file), readFile(cert_file));
        hfClient.setUserContext(user);

        channel = hfClient.newChannel("mychannel");

        Properties peerProperties = new Properties();
        peerProperties.setProperty("pemFile", peer_tls_cert_file);  // 指定TLS证书路径
        peerProperties.setProperty("sslProvider", "openSSL");
        peerProperties.setProperty("negotiationType", "TLS");
        Peer peer = hfClient.newPeer("peer0.org1.example.com", "grpcs://localhost:7051", peerProperties);

        Properties ordererProperties = new Properties();
        ordererProperties.setProperty("pemFile", orderer_tls_cert_file);  // 指定Orderer的TLS证书路径
        ordererProperties.setProperty("sslProvider", "openSSL");
        ordererProperties.setProperty("negotiationType", "TLS");
        Orderer orderer = hfClient.newOrderer("orderer.example.com", "grpcs://localhost:7050", ordererProperties);


        channel.addPeer(peer);
        channel.addOrderer(orderer);
        channel.initialize();

        BlockchainInfo blockchainInfo = channel.queryBlockchainInfo();
        long height = blockchainInfo.getHeight();
        System.out.println("区块链高度: " + height);
    }

    public static String readFile(String filePath) throws IOException {
        String ret = new String(Files.readAllBytes(Paths.get(filePath)));
        System.out.println(ret);
        return ret;
    }

    @Override
    public boolean isConnected() throws ExecutionException, InterruptedException, IOException {
        return false;
    }

    @Override
    public void close() {

    }
}
