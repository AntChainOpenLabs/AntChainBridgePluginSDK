package org.example.plugintestrunner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public class Web3jTest {

    private Web3j web3j;
    private List<String> accounts;

    private final String PRIVATE_KEY = "b661d073a71ecaafc8079e9463ecb75350b02890a9c98f9a288f0d00c99d77c3";
    private final String URL = "http://localhost:7545";
    @BeforeEach
    public void init() {
        web3j = Web3j.build(new HttpService(URL));
    }

    @Test
    public void testConnection() throws IOException {
        String web3ClientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
        System.out.println("version: " + web3ClientVersion);
    }

    @Test
    public void testGetAccounts() throws IOException {
        List<String> accounts = getAccounts();
        for (String account : accounts) {
            System.out.println("account: " + account);
        }
    }

    @Test
    public void testCreateCredential() throws IOException{
        List<String> accounts = getAccounts();
        // 创建 crendentials
        Credentials credentials = Credentials.create(PRIVATE_KEY);
        System.out.println("address: " + credentials.getAddress());
        // 获取余额
        BigInteger balance = web3j.ethGetBalance(accounts.get(0), DefaultBlockParameterName.LATEST).send().getBalance();
        System.out.println("balance: " + balance);
    }

    private List<String> getAccounts() throws IOException{
        EthAccounts ethAccounts = web3j.ethAccounts().send();
        return ethAccounts.getAccounts();
    }
}
