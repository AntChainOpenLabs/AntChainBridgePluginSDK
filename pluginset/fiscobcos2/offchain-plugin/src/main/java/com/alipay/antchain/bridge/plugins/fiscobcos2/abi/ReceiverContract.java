package com.alipay.antchain.bridge.plugins.fiscobcos2.abi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.fisco.bcos.sdk.abi.FunctionReturnDecoder;
import org.fisco.bcos.sdk.abi.TypeReference;
import org.fisco.bcos.sdk.abi.datatypes.DynamicBytes;
import org.fisco.bcos.sdk.abi.datatypes.Event;
import org.fisco.bcos.sdk.abi.datatypes.Function;
import org.fisco.bcos.sdk.abi.datatypes.Type;
import org.fisco.bcos.sdk.abi.datatypes.Utf8String;
import org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.abi.datatypes.generated.tuples.generated.Tuple3;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.contract.Contract;
import org.fisco.bcos.sdk.crypto.CryptoSuite;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.eventsub.EventCallback;
import org.fisco.bcos.sdk.model.CryptoType;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.fisco.bcos.sdk.model.callback.TransactionCallback;
import org.fisco.bcos.sdk.transaction.model.exception.ContractException;

@SuppressWarnings("unchecked")
public class ReceiverContract extends Contract {
    public static final String[] BINARY_ARRAY = {"608060405234801561001057600080fd5b50610874806100206000396000f300608060405260043610610062576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806335cd96e01461006757806389c9b1d9146100f7578063c09b261b14610187578063ff098be714610244575b600080fd5b34801561007357600080fd5b5061007c610301565b6040518080602001828103825283818151815260200191508051906020019080838360005b838110156100bc5780820151818401526020810190506100a1565b50505050905090810190601f1680156100e95780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561010357600080fd5b5061010c6103a3565b6040518080602001828103825283818151815260200191508051906020019080838360005b8381101561014c578082015181840152602081019050610131565b50505050905090810190601f1680156101795780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561019357600080fd5b50610242600480360381019080803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192908035600019169060200190929190803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290505050610445565b005b34801561025057600080fd5b506102ff600480360381019080803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192908035600019169060200190929190803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192905050506105f4565b005b606060008054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156103995780601f1061036e57610100808354040283529160200191610399565b820191906000526020600020905b81548152906001019060200180831161037c57829003601f168201915b5050505050905090565b606060018054600181600116156101000203166002900480601f01602080910402602001604051908101604052809291908181526020018280546001816001161561010002031660029004801561043b5780601f106104105761010080835404028352916020019161043b565b820191906000526020600020905b81548152906001019060200180831161041e57829003601f168201915b5050505050905090565b60208151141515156104bf576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260038152602001807f333242000000000000000000000000000000000000000000000000000000000081525060200191505060405180910390fd5b80600090805190602001906104d59291906107a3565b507f51f92d38e474586a945dac6ef7908ea588cfbe236a616f355039d447309691848383836040518080602001846000191660001916815260200180602001838103835286818151815260200191508051906020019080838360005b8381101561054c578082015181840152602081019050610531565b50505050905090810190601f1680156105795780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b838110156105b2578082015181840152602081019050610597565b50505050905090810190601f1680156105df5780820380516001836020036101000a031916815260200191505b509550505050505060405180910390a1505050565b602081511415151561066e576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260038152602001807f333242000000000000000000000000000000000000000000000000000000000081525060200191505060405180910390fd5b80600190805190602001906106849291906107a3565b507f51f92d38e474586a945dac6ef7908ea588cfbe236a616f355039d447309691848383836040518080602001846000191660001916815260200180602001838103835286818151815260200191508051906020019080838360005b838110156106fb5780820151818401526020810190506106e0565b50505050905090810190601f1680156107285780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b83811015610761578082015181840152602081019050610746565b50505050905090810190601f16801561078e5780820380516001836020036101000a031916815260200191505b509550505050505060405180910390a1505050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106107e457805160ff1916838001178555610812565b82800160010185558215610812579182015b828111156108115782518255916020019190600101906107f6565b5b50905061081f9190610823565b5090565b61084591905b80821115610841576000816000905550600101610829565b5090565b905600a165627a7a72305820a972cf2fc660f09e8d8c957a4dac25ec0b6f426523fc9956737de29b1b7597ba0029"};

    public static final String BINARY = org.fisco.bcos.sdk.utils.StringUtils.joinAll("", BINARY_ARRAY);

    public static final String[] SM_BINARY_ARRAY = {"608060405234801561001057600080fd5b50610874806100206000396000f300608060405260043610610062576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff168063281cf5e6146100675780638a58513714610124578063bdb560a6146101b4578063e5b813ad14610244575b600080fd5b34801561007357600080fd5b50610122600480360381019080803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192908035600019169060200190929190803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290505050610301565b005b34801561013057600080fd5b506101396104b0565b6040518080602001828103825283818151815260200191508051906020019080838360005b8381101561017957808201518184015260208101905061015e565b50505050905090810190601f1680156101a65780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b3480156101c057600080fd5b506101c9610552565b6040518080602001828103825283818151815260200191508051906020019080838360005b838110156102095780820151818401526020810190506101ee565b50505050905090810190601f1680156102365780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561025057600080fd5b506102ff600480360381019080803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192908035600019169060200190929190803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192905050506105f4565b005b602081511415151561037b576040517fc703cb120000000000000000000000000000000000000000000000000000000081526004018080602001828103825260038152602001807f333242000000000000000000000000000000000000000000000000000000000081525060200191505060405180910390fd5b80600090805190602001906103919291906107a3565b507fb3585d51c91acc32851a939fc4fdfcd2d40d9a7fe411adc66400a9d411677ea08383836040518080602001846000191660001916815260200180602001838103835286818151815260200191508051906020019080838360005b838110156104085780820151818401526020810190506103ed565b50505050905090810190601f1680156104355780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b8381101561046e578082015181840152602081019050610453565b50505050905090810190601f16801561049b5780820380516001836020036101000a031916815260200191505b509550505050505060405180910390a1505050565b606060018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156105485780601f1061051d57610100808354040283529160200191610548565b820191906000526020600020905b81548152906001019060200180831161052b57829003601f168201915b5050505050905090565b606060008054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156105ea5780601f106105bf576101008083540402835291602001916105ea565b820191906000526020600020905b8154815290600101906020018083116105cd57829003601f168201915b5050505050905090565b602081511415151561066e576040517fc703cb120000000000000000000000000000000000000000000000000000000081526004018080602001828103825260038152602001807f333242000000000000000000000000000000000000000000000000000000000081525060200191505060405180910390fd5b80600190805190602001906106849291906107a3565b507fb3585d51c91acc32851a939fc4fdfcd2d40d9a7fe411adc66400a9d411677ea08383836040518080602001846000191660001916815260200180602001838103835286818151815260200191508051906020019080838360005b838110156106fb5780820151818401526020810190506106e0565b50505050905090810190601f1680156107285780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b83811015610761578082015181840152602081019050610746565b50505050905090810190601f16801561078e5780820380516001836020036101000a031916815260200191505b509550505050505060405180910390a1505050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106107e457805160ff1916838001178555610812565b82800160010185558215610812579182015b828111156108115782518255916020019190600101906107f6565b5b50905061081f9190610823565b5090565b61084591905b80821115610841576000816000905550600101610829565b5090565b905600a165627a7a7230582085a79e379f99591553d5dc85daa8066f728f973195d6b726857ab1b749579cd10029"};

    public static final String SM_BINARY = org.fisco.bcos.sdk.utils.StringUtils.joinAll("", SM_BINARY_ARRAY);

    public static final String[] ABI_ARRAY = {"[{\"constant\":true,\"inputs\":[],\"name\":\"getLastMsg\",\"outputs\":[{\"name\":\"\",\"type\":\"bytes\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"getLastUnorderedMsg\",\"outputs\":[{\"name\":\"\",\"type\":\"bytes\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"domain_name\",\"type\":\"string\"},{\"name\":\"author\",\"type\":\"bytes32\"},{\"name\":\"message\",\"type\":\"bytes\"}],\"name\":\"recvMessage\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[{\"name\":\"domain_name\",\"type\":\"string\"},{\"name\":\"author\",\"type\":\"bytes32\"},{\"name\":\"message\",\"type\":\"bytes\"}],\"name\":\"recvUnorderedMessage\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"key\",\"type\":\"string\"},{\"indexed\":false,\"name\":\"value\",\"type\":\"bytes32\"},{\"indexed\":false,\"name\":\"enterprise\",\"type\":\"string\"}],\"name\":\"amNotify\",\"type\":\"event\"}]"};

    public static final String ABI = org.fisco.bcos.sdk.utils.StringUtils.joinAll("", ABI_ARRAY);

    public static final String FUNC_GETLASTMSG = "getLastMsg";

    public static final String FUNC_GETLASTUNORDEREDMSG = "getLastUnorderedMsg";

    public static final String FUNC_RECVMESSAGE = "recvMessage";

    public static final String FUNC_RECVUNORDEREDMESSAGE = "recvUnorderedMessage";

    public static final Event AMNOTIFY_EVENT = new Event("amNotify", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Utf8String>() {}));
    ;

    protected ReceiverContract(String contractAddress, Client client, CryptoKeyPair credential) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, credential);
    }

    public static String getBinary(CryptoSuite cryptoSuite) {
        return (cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE ? BINARY : SM_BINARY);
    }

    public byte[] getLastMsg() throws ContractException {
        final Function function = new Function(FUNC_GETLASTMSG, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
        return executeCallWithSingleValueReturn(function, byte[].class);
    }

    public byte[] getLastUnorderedMsg() throws ContractException {
        final Function function = new Function(FUNC_GETLASTUNORDEREDMSG, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
        return executeCallWithSingleValueReturn(function, byte[].class);
    }

    public TransactionReceipt recvMessage(String domain_name, byte[] author, byte[] message) {
        final Function function = new Function(
                FUNC_RECVMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.abi.datatypes.Utf8String(domain_name), 
                new org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32(author), 
                new org.fisco.bcos.sdk.abi.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList());
        return executeTransaction(function);
    }

    public byte[] recvMessage(String domain_name, byte[] author, byte[] message, TransactionCallback callback) {
        final Function function = new Function(
                FUNC_RECVMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.abi.datatypes.Utf8String(domain_name), 
                new org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32(author), 
                new org.fisco.bcos.sdk.abi.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList());
        return asyncExecuteTransaction(function, callback);
    }

    public String getSignedTransactionForRecvMessage(String domain_name, byte[] author, byte[] message) {
        final Function function = new Function(
                FUNC_RECVMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.abi.datatypes.Utf8String(domain_name), 
                new org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32(author), 
                new org.fisco.bcos.sdk.abi.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList());
        return createSignedTransaction(function);
    }

    public Tuple3<String, byte[], byte[]> getRecvMessageInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_RECVMESSAGE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}, new TypeReference<DynamicBytes>() {}));
        List<Type> results = FunctionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<String, byte[], byte[]>(

                (String) results.get(0).getValue(), 
                (byte[]) results.get(1).getValue(), 
                (byte[]) results.get(2).getValue()
                );
    }

    public TransactionReceipt recvUnorderedMessage(String domain_name, byte[] author, byte[] message) {
        final Function function = new Function(
                FUNC_RECVUNORDEREDMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.abi.datatypes.Utf8String(domain_name), 
                new org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32(author), 
                new org.fisco.bcos.sdk.abi.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList());
        return executeTransaction(function);
    }

    public byte[] recvUnorderedMessage(String domain_name, byte[] author, byte[] message, TransactionCallback callback) {
        final Function function = new Function(
                FUNC_RECVUNORDEREDMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.abi.datatypes.Utf8String(domain_name), 
                new org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32(author), 
                new org.fisco.bcos.sdk.abi.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList());
        return asyncExecuteTransaction(function, callback);
    }

    public String getSignedTransactionForRecvUnorderedMessage(String domain_name, byte[] author, byte[] message) {
        final Function function = new Function(
                FUNC_RECVUNORDEREDMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.abi.datatypes.Utf8String(domain_name), 
                new org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32(author), 
                new org.fisco.bcos.sdk.abi.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList());
        return createSignedTransaction(function);
    }

    public Tuple3<String, byte[], byte[]> getRecvUnorderedMessageInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_RECVUNORDEREDMESSAGE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}, new TypeReference<DynamicBytes>() {}));
        List<Type> results = FunctionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<String, byte[], byte[]>(

                (String) results.get(0).getValue(), 
                (byte[]) results.get(1).getValue(), 
                (byte[]) results.get(2).getValue()
                );
    }

    public List<AmNotifyEventResponse> getAmNotifyEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(AMNOTIFY_EVENT, transactionReceipt);
        ArrayList<AmNotifyEventResponse> responses = new ArrayList<AmNotifyEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AmNotifyEventResponse typedResponse = new AmNotifyEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.key = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.value = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.enterprise = (String) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeAmNotifyEvent(String fromBlock, String toBlock, List<String> otherTopics, EventCallback callback) {
        String topic0 = eventEncoder.encode(AMNOTIFY_EVENT);
        subscribeEvent(ABI,BINARY,topic0,fromBlock,toBlock,otherTopics,callback);
    }

    public void subscribeAmNotifyEvent(EventCallback callback) {
        String topic0 = eventEncoder.encode(AMNOTIFY_EVENT);
        subscribeEvent(ABI,BINARY,topic0,callback);
    }

    public static ReceiverContract load(String contractAddress, Client client, CryptoKeyPair credential) {
        return new ReceiverContract(contractAddress, client, credential);
    }

    public static ReceiverContract deploy(Client client, CryptoKeyPair credential) throws ContractException {
        return deploy(ReceiverContract.class, client, credential, getBinary(client.getCryptoSuite()), "");
    }

    public static class AmNotifyEventResponse {
        public TransactionReceipt.Logs log;

        public String key;

        public byte[] value;

        public String enterprise;
    }
}
