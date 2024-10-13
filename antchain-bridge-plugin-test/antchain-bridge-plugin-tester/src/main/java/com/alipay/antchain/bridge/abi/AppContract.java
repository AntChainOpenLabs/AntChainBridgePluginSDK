package com.alipay.antchain.bridge.abi;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.9.4.
 */
@SuppressWarnings("rawtypes")
public class AppContract extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b50600080546001600160a01b031916339081178255604051909182917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0908290a350610c19806100616000396000f3fe608060405234801561001057600080fd5b50600436106100f55760003560e01c80639670efcb11610097578063c1cecc5a11610066578063c1cecc5a146101c2578063f2fde38b146101d5578063f76f703b146101e8578063ff098be7146101fb57600080fd5b80639670efcb1461018c578063a25ae14a1461019f578063aec2fc14146101a7578063c09b261b146101af57600080fd5b80633fecfe3f116100d35780633fecfe3f14610158578063715018a61461016b57806389c9b1d9146101735780638da5cb5b1461017b57600080fd5b80630a9d793d146100fa57806335cd96e01461010f578063387868ae1461012d575b600080fd5b61010d610108366004610929565b61020e565b005b610117610263565b60405161012491906109a6565b60405180910390f35b600354610140906001600160a01b031681565b6040516001600160a01b039091168152602001610124565b6101176101663660046109b9565b6102f5565b61010d6103ae565b610117610422565b6000546001600160a01b0316610140565b61011761019a3660046109b9565b610431565b61011761044d565b61011761045a565b61010d6101bd366004610a67565b610467565b61010d6101d0366004610a67565b61053f565b61010d6101e3366004610929565b61060d565b61010d6101f6366004610a67565b6106f7565b61010d610209366004610a67565b6107c5565b6000546001600160a01b031633146102415760405162461bcd60e51b815260040161023890610aff565b60405180910390fd5b600380546001600160a01b0319166001600160a01b0392909216919091179055565b60606004805461027290610b34565b80601f016020809104026020016040519081016040528092919081815260200182805461029e90610b34565b80156102eb5780601f106102c0576101008083540402835291602001916102eb565b820191906000526020600020905b8154815290600101906020018083116102ce57829003601f168201915b5050505050905090565b6002602052816000526040600020818154811061031157600080fd5b9060005260206000200160009150915050805461032d90610b34565b80601f016020809104026020016040519081016040528092919081815260200182805461035990610b34565b80156103a65780601f1061037b576101008083540402835291602001916103a6565b820191906000526020600020905b81548152906001019060200180831161038957829003601f168201915b505050505081565b6000546001600160a01b031633146103d85760405162461bcd60e51b815260040161023890610aff565b600080546040516001600160a01b03909116907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0908390a3600080546001600160a01b0319169055565b60606005805461027290610b34565b6001602052816000526040600020818154811061031157600080fd5b6005805461032d90610b34565b6004805461032d90610b34565b6003546001600160a01b031633146104b65760405162461bcd60e51b815260206004820152601260248201527124a72b20a624a22fa822a926a4a9a9a4a7a760711b6044820152606401610238565b600082815260016020818152604083208054928301815583529182902083516104e793919092019190840190610890565b5080516104fb906004906020840190610890565b507f7b59e766dd02d6ce4e574f0ab75dfc4b180c90deb50dd9dbdbc65768abcf80f183838360016040516105329493929190610b6f565b60405180910390a1505050565b6003546040516360e7662d60e11b81526001600160a01b039091169063c1cecc5a9061057390869086908690600401610bae565b600060405180830381600087803b15801561058d57600080fd5b505af11580156105a1573d6000803e3d6000fd5b5050506000838152600260209081526040822080546001810182559083529181902084516105d6945092019190840190610890565b507fbbe83d8459c305cf51f2425b93e693dcfaaf60f22766486b0983c905b98ead4083838360006040516105329493929190610b6f565b6000546001600160a01b031633146106375760405162461bcd60e51b815260040161023890610aff565b6001600160a01b03811661069c5760405162461bcd60e51b815260206004820152602660248201527f4f776e61626c653a206e6577206f776e657220697320746865207a65726f206160448201526564647265737360d01b6064820152608401610238565b600080546040516001600160a01b03808516939216917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e091a3600080546001600160a01b0319166001600160a01b0392909216919091179055565b60035460405163f76f703b60e01b81526001600160a01b039091169063f76f703b9061072b90869086908690600401610bae565b600060405180830381600087803b15801561074557600080fd5b505af1158015610759573d6000803e3d6000fd5b50505060008381526002602090815260408220805460018101825590835291819020845161078e945092019190840190610890565b507fbbe83d8459c305cf51f2425b93e693dcfaaf60f22766486b0983c905b98ead4083838360016040516105329493929190610b6f565b6003546001600160a01b031633146108145760405162461bcd60e51b815260206004820152601260248201527124a72b20a624a22fa822a926a4a9a9a4a7a760711b6044820152606401610238565b6000828152600160208181526040832080549283018155835291829020835161084593919092019190840190610890565b508051610859906005906020840190610890565b507f7b59e766dd02d6ce4e574f0ab75dfc4b180c90deb50dd9dbdbc65768abcf80f183838360006040516105329493929190610b6f565b82805461089c90610b34565b90600052602060002090601f0160209004810192826108be5760008555610904565b82601f106108d757805160ff1916838001178555610904565b82800160010185558215610904579182015b828111156109045782518255916020019190600101906108e9565b50610910929150610914565b5090565b5b808211156109105760008155600101610915565b60006020828403121561093b57600080fd5b81356001600160a01b038116811461095257600080fd5b9392505050565b6000815180845260005b8181101561097f57602081850181015186830182015201610963565b81811115610991576000602083870101525b50601f01601f19169290920160200192915050565b6020815260006109526020830184610959565b600080604083850312156109cc57600080fd5b50508035926020909101359150565b634e487b7160e01b600052604160045260246000fd5b600067ffffffffffffffff80841115610a0c57610a0c6109db565b604051601f8501601f19908116603f01168101908282118183101715610a3457610a346109db565b81604052809350858152868686011115610a4d57600080fd5b858560208301376000602087830101525050509392505050565b600080600060608486031215610a7c57600080fd5b833567ffffffffffffffff80821115610a9457600080fd5b818601915086601f830112610aa857600080fd5b610ab7878335602085016109f1565b9450602086013593506040860135915080821115610ad457600080fd5b508401601f81018613610ae657600080fd5b610af5868235602084016109f1565b9150509250925092565b6020808252818101527f4f776e61626c653a2063616c6c6572206973206e6f7420746865206f776e6572604082015260600190565b600181811c90821680610b4857607f821691505b60208210811415610b6957634e487b7160e01b600052602260045260246000fd5b50919050565b608081526000610b826080830187610959565b8560208401528281036040840152610b9a8186610959565b915050821515606083015295945050505050565b606081526000610bc16060830186610959565b8460208401528281036040840152610bd98185610959565b969550505050505056fea2646970667358221220febd3cc4b2611a5ef30460911ff42fa1255ff73573bd6eef96c373a804e6409264736f6c63430008090033";

    public static final String FUNC_GETLASTMSG = "getLastMsg";

    public static final String FUNC_GETLASTUNORDEREDMSG = "getLastUnorderedMsg";

    public static final String FUNC_LAST_MSG = "last_msg";

    public static final String FUNC_LAST_UO_MSG = "last_uo_msg";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_RECVMESSAGE = "recvMessage";

    public static final String FUNC_RECVMSG = "recvMsg";

    public static final String FUNC_RECVUNORDEREDMESSAGE = "recvUnorderedMessage";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_SDPADDRESS = "sdpAddress";

    public static final String FUNC_SENDMESSAGE = "sendMessage";

    public static final String FUNC_SENDMSG = "sendMsgUnordered";

    public static final String FUNC_SENDUNORDEREDMESSAGE = "sendUnorderedMessage";

    public static final String FUNC_SETPROTOCOL = "setProtocol";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event RECVCROSSCHAINMSG_EVENT = new Event("recvCrosschainMsg",
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}, new TypeReference<DynamicBytes>() {}, new TypeReference<Bool>() {}));
    ;

    public static final Event SENDCROSSCHAINMSG_EVENT = new Event("sendCrosschainMsg",
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}, new TypeReference<DynamicBytes>() {}, new TypeReference<Bool>() {}));
    ;

    @Deprecated
    protected AppContract(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected AppContract(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected AppContract(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected AppContract(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<OwnershipTransferredEventResponse> getOwnershipTransferredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, transactionReceipt);
        ArrayList<OwnershipTransferredEventResponse> responses = new ArrayList<OwnershipTransferredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.previousOwner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.newOwner = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, OwnershipTransferredEventResponse>() {
            @Override
            public OwnershipTransferredEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, log);
                OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
                typedResponse.log = log;
                typedResponse.previousOwner = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.newOwner = (String) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<OwnershipTransferredEventResponse> ownershipTransferredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OWNERSHIPTRANSFERRED_EVENT));
        return ownershipTransferredEventFlowable(filter);
    }

    public static List<RecvCrosschainMsgEventResponse> getRecvCrosschainMsgEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(RECVCROSSCHAINMSG_EVENT, transactionReceipt);
        ArrayList<RecvCrosschainMsgEventResponse> responses = new ArrayList<RecvCrosschainMsgEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RecvCrosschainMsgEventResponse typedResponse = new RecvCrosschainMsgEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.senderDomain = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.author = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.message = (byte[]) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.isOrdered = (Boolean) eventValues.getNonIndexedValues().get(3).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<RecvCrosschainMsgEventResponse> recvCrosschainMsgEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, RecvCrosschainMsgEventResponse>() {
            @Override
            public RecvCrosschainMsgEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(RECVCROSSCHAINMSG_EVENT, log);
                RecvCrosschainMsgEventResponse typedResponse = new RecvCrosschainMsgEventResponse();
                typedResponse.log = log;
                typedResponse.senderDomain = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.author = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.message = (byte[]) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.isOrdered = (Boolean) eventValues.getNonIndexedValues().get(3).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<RecvCrosschainMsgEventResponse> recvCrosschainMsgEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RECVCROSSCHAINMSG_EVENT));
        return recvCrosschainMsgEventFlowable(filter);
    }

    public static List<SendCrosschainMsgEventResponse> getSendCrosschainMsgEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SENDCROSSCHAINMSG_EVENT, transactionReceipt);
        ArrayList<SendCrosschainMsgEventResponse> responses = new ArrayList<SendCrosschainMsgEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SendCrosschainMsgEventResponse typedResponse = new SendCrosschainMsgEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.receiverDomain = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.receiver = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.message = (byte[]) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.isOrdered = (Boolean) eventValues.getNonIndexedValues().get(3).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<SendCrosschainMsgEventResponse> sendCrosschainMsgEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, SendCrosschainMsgEventResponse>() {
            @Override
            public SendCrosschainMsgEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SENDCROSSCHAINMSG_EVENT, log);
                SendCrosschainMsgEventResponse typedResponse = new SendCrosschainMsgEventResponse();
                typedResponse.log = log;
                typedResponse.receiverDomain = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.receiver = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.message = (byte[]) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.isOrdered = (Boolean) eventValues.getNonIndexedValues().get(3).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<SendCrosschainMsgEventResponse> sendCrosschainMsgEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SENDCROSSCHAINMSG_EVENT));
        return sendCrosschainMsgEventFlowable(filter);
    }

    public RemoteFunctionCall<byte[]> getLastMsg() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETLASTMSG,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> getLastUnorderedMsg() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETLASTUNORDEREDMSG,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> last_msg() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_LAST_MSG,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> last_uo_msg() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_LAST_UO_MSG,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<String> owner() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_OWNER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> recvMessage(String senderDomain, byte[] author, byte[] message) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_RECVMESSAGE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(senderDomain),
                        new org.web3j.abi.datatypes.generated.Bytes32(author),
                        new org.web3j.abi.datatypes.DynamicBytes(message)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<byte[]> recvMsg(byte[] param0, BigInteger param1) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_RECVMSG,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0),
                        new org.web3j.abi.datatypes.generated.Uint256(param1)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> recvUnorderedMessage(String senderDomain, byte[] author, byte[] message) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_RECVUNORDEREDMESSAGE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(senderDomain),
                        new org.web3j.abi.datatypes.generated.Bytes32(author),
                        new org.web3j.abi.datatypes.DynamicBytes(message)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> renounceOwnership() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_RENOUNCEOWNERSHIP,
                Arrays.<Type>asList(),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> sdpAddress() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SDPADDRESS,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> sendMessage(String receiverDomain, byte[] receiver, byte[] message) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SENDMESSAGE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(receiverDomain),
                        new org.web3j.abi.datatypes.generated.Bytes32(receiver),
                        new org.web3j.abi.datatypes.DynamicBytes(message)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<byte[]> sendMsg(byte[] param0, BigInteger param1) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SENDMSG,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0),
                        new org.web3j.abi.datatypes.generated.Uint256(param1)),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> sendUnorderedMessage(String receiverDomain, byte[] receiver, byte[] message) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SENDUNORDEREDMESSAGE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(receiverDomain),
                        new org.web3j.abi.datatypes.generated.Bytes32(receiver),
                        new org.web3j.abi.datatypes.DynamicBytes(message)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setProtocol(String protocolAddress) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETPROTOCOL,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, protocolAddress)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> transferOwnership(String newOwner) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_TRANSFEROWNERSHIP,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, newOwner)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static AppContract load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new AppContract(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static AppContract load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new AppContract(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static AppContract load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new AppContract(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static AppContract load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new AppContract(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<AppContract> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(AppContract.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<AppContract> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(AppContract.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<AppContract> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(AppContract.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<AppContract> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(AppContract.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public String previousOwner;

        public String newOwner;
    }

    public static class RecvCrosschainMsgEventResponse extends BaseEventResponse {
        public String senderDomain;

        public byte[] author;

        public byte[] message;

        public Boolean isOrdered;
    }

    public static class SendCrosschainMsgEventResponse extends BaseEventResponse {
        public String receiverDomain;

        public byte[] receiver;

        public byte[] message;

        public Boolean isOrdered;
    }
}
