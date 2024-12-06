package com.alipay.antchain.bridge.plugins.fiscobcos2.abi;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.fisco.bcos.sdk.abi.FunctionReturnDecoder;
import org.fisco.bcos.sdk.abi.TypeReference;
import org.fisco.bcos.sdk.abi.datatypes.Address;
import org.fisco.bcos.sdk.abi.datatypes.DynamicBytes;
import org.fisco.bcos.sdk.abi.datatypes.Function;
import org.fisco.bcos.sdk.abi.datatypes.Type;
import org.fisco.bcos.sdk.abi.datatypes.Utf8String;
import org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.abi.datatypes.generated.tuples.generated.Tuple1;
import org.fisco.bcos.sdk.abi.datatypes.generated.tuples.generated.Tuple3;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.contract.Contract;
import org.fisco.bcos.sdk.crypto.CryptoSuite;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.model.CryptoType;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.fisco.bcos.sdk.model.callback.TransactionCallback;
import org.fisco.bcos.sdk.transaction.model.exception.ContractException;

@SuppressWarnings("unchecked")
public class SenderContract extends Contract {
    public static final String[] BINARY_ARRAY = {"608060405234801561001057600080fd5b506105b2806100206000396000f300608060405260043610610057576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806340610fd61461005c57806356cc9208146101195780636f9009de146101d6575b600080fd5b34801561006857600080fd5b506101176004803603810190808035600019169060200190929190803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290505050610219565b005b34801561012557600080fd5b506101d46004803603810190808035600019169060200190929190803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192905050506103ae565b005b3480156101e257600080fd5b50610217600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610543565b005b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508073ffffffffffffffffffffffffffffffffffffffff1663c1cecc5a8486856040518463ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018080602001846000191660001916815260200180602001838103835286818151815260200191508051906020019080838360005b838110156102db5780820151818401526020810190506102c0565b50505050905090810190601f1680156103085780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b83811015610341578082015181840152602081019050610326565b50505050905090810190601f16801561036e5780820380516001836020036101000a031916815260200191505b5095505050505050600060405180830381600087803b15801561039057600080fd5b505af11580156103a4573d6000803e3d6000fd5b5050505050505050565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508073ffffffffffffffffffffffffffffffffffffffff1663f76f703b8486856040518463ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018080602001846000191660001916815260200180602001838103835286818151815260200191508051906020019080838360005b83811015610470578082015181840152602081019050610455565b50505050905090810190601f16801561049d5780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b838110156104d65780820151818401526020810190506104bb565b50505050905090810190601f1680156105035780820380516001836020036101000a031916815260200191505b5095505050505050600060405180830381600087803b15801561052557600080fd5b505af1158015610539573d6000803e3d6000fd5b5050505050505050565b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550505600a165627a7a72305820544e0a8be65e13a3c96c788153f3cff5a185155f198a6f423b0359025c8c77800029"};

    public static final String BINARY = org.fisco.bcos.sdk.utils.StringUtils.joinAll("", BINARY_ARRAY);

    public static final String[] SM_BINARY_ARRAY = {"608060405234801561001057600080fd5b506105b2806100206000396000f300608060405260043610610057576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680634d5081101461005c5780636f0f92091461009f578063e029e9fc1461015c575b600080fd5b34801561006857600080fd5b5061009d600480360381019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610219565b005b3480156100ab57600080fd5b5061015a6004803603810190808035600019169060200190929190803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290803590602001908201803590602001908080601f016020809104026020016040519081016040528093929190818152602001838380828437820191505050505050919291929050505061025c565b005b34801561016857600080fd5b506102176004803603810190808035600019169060200190929190803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192905050506103f1565b005b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508073ffffffffffffffffffffffffffffffffffffffff166365699d8d8486856040518463ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018080602001846000191660001916815260200180602001838103835286818151815260200191508051906020019080838360005b8381101561031e578082015181840152602081019050610303565b50505050905090810190601f16801561034b5780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b83811015610384578082015181840152602081019050610369565b50505050905090810190601f1680156103b15780820380516001836020036101000a031916815260200191505b5095505050505050600060405180830381600087803b1580156103d357600080fd5b505af11580156103e7573d6000803e3d6000fd5b5050505050505050565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690508073ffffffffffffffffffffffffffffffffffffffff16630204ebc28486856040518463ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018080602001846000191660001916815260200180602001838103835286818151815260200191508051906020019080838360005b838110156104b3578082015181840152602081019050610498565b50505050905090810190601f1680156104e05780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b838110156105195780820151818401526020810190506104fe565b50505050905090810190601f1680156105465780820380516001836020036101000a031916815260200191505b5095505050505050600060405180830381600087803b15801561056857600080fd5b505af115801561057c573d6000803e3d6000fd5b50505050505050505600a165627a7a723058208f80400f41bbf080edbb921e11792861b919418019c5dc7022d7cd6b619dcac30029"};

    public static final String SM_BINARY = org.fisco.bcos.sdk.utils.StringUtils.joinAll("", SM_BINARY_ARRAY);

    public static final String[] ABI_ARRAY = {"[{\"constant\":false,\"inputs\":[{\"name\":\"receiver\",\"type\":\"bytes32\"},{\"name\":\"domain\",\"type\":\"string\"},{\"name\":\"_msg\",\"type\":\"bytes\"}],\"name\":\"sendUnordered\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"receiver\",\"type\":\"bytes32\"},{\"name\":\"domain\",\"type\":\"string\"},{\"name\":\"_msg\",\"type\":\"bytes\"}],\"name\":\"send\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"_sdp_address\",\"type\":\"address\"}],\"name\":\"setSdpMSGAddress\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]"};

    public static final String ABI = org.fisco.bcos.sdk.utils.StringUtils.joinAll("", ABI_ARRAY);

    public static final String FUNC_SENDUNORDERED = "sendUnordered";

    public static final String FUNC_SEND = "send";

    public static final String FUNC_SETSDPMSGADDRESS = "setSdpMSGAddress";

    protected SenderContract(String contractAddress, Client client, CryptoKeyPair credential) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, credential);
    }

    public static String getBinary(CryptoSuite cryptoSuite) {
        return (cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE ? BINARY : SM_BINARY);
    }

    public TransactionReceipt sendUnordered(byte[] receiver, String domain, byte[] _msg) {
        final Function function = new Function(
                FUNC_SENDUNORDERED, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32(receiver), 
                new org.fisco.bcos.sdk.abi.datatypes.Utf8String(domain), 
                new org.fisco.bcos.sdk.abi.datatypes.DynamicBytes(_msg)), 
                Collections.<TypeReference<?>>emptyList());
        return executeTransaction(function);
    }

    public byte[] sendUnordered(byte[] receiver, String domain, byte[] _msg, TransactionCallback callback) {
        final Function function = new Function(
                FUNC_SENDUNORDERED, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32(receiver), 
                new org.fisco.bcos.sdk.abi.datatypes.Utf8String(domain), 
                new org.fisco.bcos.sdk.abi.datatypes.DynamicBytes(_msg)), 
                Collections.<TypeReference<?>>emptyList());
        return asyncExecuteTransaction(function, callback);
    }

    public String getSignedTransactionForSendUnordered(byte[] receiver, String domain, byte[] _msg) {
        final Function function = new Function(
                FUNC_SENDUNORDERED, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32(receiver), 
                new org.fisco.bcos.sdk.abi.datatypes.Utf8String(domain), 
                new org.fisco.bcos.sdk.abi.datatypes.DynamicBytes(_msg)), 
                Collections.<TypeReference<?>>emptyList());
        return createSignedTransaction(function);
    }

    public Tuple3<byte[], String, byte[]> getSendUnorderedInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_SENDUNORDERED, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Utf8String>() {}, new TypeReference<DynamicBytes>() {}));
        List<Type> results = FunctionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<byte[], String, byte[]>(

                (byte[]) results.get(0).getValue(), 
                (String) results.get(1).getValue(), 
                (byte[]) results.get(2).getValue()
                );
    }

    public TransactionReceipt send(byte[] receiver, String domain, byte[] _msg) {
        final Function function = new Function(
                FUNC_SEND, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32(receiver), 
                new org.fisco.bcos.sdk.abi.datatypes.Utf8String(domain), 
                new org.fisco.bcos.sdk.abi.datatypes.DynamicBytes(_msg)), 
                Collections.<TypeReference<?>>emptyList());
        return executeTransaction(function);
    }

    public byte[] send(byte[] receiver, String domain, byte[] _msg, TransactionCallback callback) {
        final Function function = new Function(
                FUNC_SEND, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32(receiver), 
                new org.fisco.bcos.sdk.abi.datatypes.Utf8String(domain), 
                new org.fisco.bcos.sdk.abi.datatypes.DynamicBytes(_msg)), 
                Collections.<TypeReference<?>>emptyList());
        return asyncExecuteTransaction(function, callback);
    }

    public String getSignedTransactionForSend(byte[] receiver, String domain, byte[] _msg) {
        final Function function = new Function(
                FUNC_SEND, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32(receiver), 
                new org.fisco.bcos.sdk.abi.datatypes.Utf8String(domain), 
                new org.fisco.bcos.sdk.abi.datatypes.DynamicBytes(_msg)), 
                Collections.<TypeReference<?>>emptyList());
        return createSignedTransaction(function);
    }

    public Tuple3<byte[], String, byte[]> getSendInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_SEND, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Utf8String>() {}, new TypeReference<DynamicBytes>() {}));
        List<Type> results = FunctionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<byte[], String, byte[]>(

                (byte[]) results.get(0).getValue(), 
                (String) results.get(1).getValue(), 
                (byte[]) results.get(2).getValue()
                );
    }

    public TransactionReceipt setSdpMSGAddress(String _sdp_address) {
        final Function function = new Function(
                FUNC_SETSDPMSGADDRESS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.abi.datatypes.Address(_sdp_address)), 
                Collections.<TypeReference<?>>emptyList());
        return executeTransaction(function);
    }

    public byte[] setSdpMSGAddress(String _sdp_address, TransactionCallback callback) {
        final Function function = new Function(
                FUNC_SETSDPMSGADDRESS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.abi.datatypes.Address(_sdp_address)), 
                Collections.<TypeReference<?>>emptyList());
        return asyncExecuteTransaction(function, callback);
    }

    public String getSignedTransactionForSetSdpMSGAddress(String _sdp_address) {
        final Function function = new Function(
                FUNC_SETSDPMSGADDRESS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.abi.datatypes.Address(_sdp_address)), 
                Collections.<TypeReference<?>>emptyList());
        return createSignedTransaction(function);
    }

    public Tuple1<String> getSetSdpMSGAddressInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_SETSDPMSGADDRESS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        List<Type> results = FunctionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<String>(

                (String) results.get(0).getValue()
                );
    }

    public static SenderContract load(String contractAddress, Client client, CryptoKeyPair credential) {
        return new SenderContract(contractAddress, client, credential);
    }

    public static SenderContract deploy(Client client, CryptoKeyPair credential) throws ContractException {
        return deploy(SenderContract.class, client, credential, getBinary(client.getCryptoSuite()), "");
    }
}
