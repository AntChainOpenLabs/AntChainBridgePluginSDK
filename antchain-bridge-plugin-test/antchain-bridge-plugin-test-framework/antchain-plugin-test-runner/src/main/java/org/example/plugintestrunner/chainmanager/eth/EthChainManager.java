package org.example.plugintestrunner.chainmanager.eth;

import lombok.Getter;
import lombok.Setter;
import org.example.plugintestrunner.chainmanager.IChainManager;
import org.example.plugintestrunner.exception.ChainManagerException;
import org.example.plugintestrunner.exception.ChainManagerException.*;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;
//import org.web3j.protocol.Web3j;
//import org.web3j.protocol.core.DefaultBlockParameterName;
//import org.web3j.protocol.core.methods.response.EthBlock;
//import org.web3j.protocol.Web3j;
//import org.web3j.protocol.http.HttpService;
//import org.web3j.protocol.core.DefaultBlockParameterName;
//import org.web3j.protocol.core.methods.response.EthBlock;

import java.io.*;
import java.math.BigInteger;


@Getter
@Setter
public class EthChainManager extends IChainManager {

    private String httpUrl;
    private String privateKey;
    private String gasPrice;
    private String gasLimit;
    private final Web3j web3j;

    public EthChainManager(String httpUrl, String privateKeyFile, String gasPrice, String gasLimit) throws ChainManagerException, IOException {
        this.httpUrl = httpUrl;
        this.web3j = Web3j.build(new HttpService(httpUrl));
        try {
            setPrivateKey(privateKeyFile);
            this.gasPrice = gasPrice;
            this.gasLimit = gasLimit;
//            setGasLimit();
            System.out.println("gasLimit: " + this.gasLimit);
//            setGasPrice();
            System.out.println("gasPrice: " + this.gasPrice);
        } catch (Exception e) {
            throw new ChainManagerConstructionException("Failed to set account or gas limit or gas price", e);
        }
        // 构造用于插件测试的配置信息
        this.config = String.format("{\"gasLimit\":%s,\"gasPrice\":%s,\"privateKey\":\"%s\",\"url\":\"%s\"}",
                this.gasLimit, this.gasPrice, this.privateKey, httpUrl);
    }

//    private void sendTransaction() throws IOException {
//        // 2. 加载账户的私钥
//        Credentials credentials = Credentials.create(privateKey);
//
//        // 3. 获取账户nonce值
//        BigInteger nonce = web3j.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST)
//                .send()
//                .getTransactionCount();
//
//        // 4. 设置交易参数：目标地址，gas价格，gas限制，交易金额
//        String toAddress = "0xe450d2f9c4740b987f836cada758fb3c35c6d9d7";  // 目标地址
//        BigInteger value = Convert.toWei("0.01", Convert.Unit.ETHER).toBigInteger();  // 发送1 Ether
//        BigInteger gasPrice = BigInteger.valueOf(Long.valueOf(this.gasPrice));  // 20 Gwei
//        BigInteger gasLimit = BigInteger.valueOf(Long.valueOf(this.gasLimit));  // Gas 限制
//
//        // 5. 创建交易对象
//        RawTransaction rawTransaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, value);
//
//        // 6. 对交易进行签名
//        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
//        String hexValue = Numeric.toHexString(signedMessage);
//
//        // 7. 发送交易
//        EthSendTransaction transactionResponse = web3j.ethSendRawTransaction(hexValue).send();
//
//        // 8. 获取交易Hash
//        String transactionHash = transactionResponse.getTransactionHash();
//        System.out.println("Transaction hash: " + transactionHash);
//    }



    // 通过文件获取
    public void setPrivateKey(String privateKeyFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(privateKeyFile));
        this.privateKey = br.readLine();
    }

    // 通过 web3j 获取
    public void setGasPrice() throws IOException {
        this.gasPrice = this.web3j.ethGasPrice().send().getGasPrice().toString();
    }

//     通过 web3j 获取
    public void setGasLimit() throws IOException {
        EthBlock latestBlock = this.web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send();
        this.gasLimit = latestBlock.getBlock().getGasLimit().toString();
    }

    @Override
    public void close() {
//        this.web3j.shutdown();
    }
}
