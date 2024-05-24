package com.alipay.antchain.bridge.plugins.fiscobcos.abi;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.codec.datatypes.Address;
import org.fisco.bcos.sdk.v3.codec.datatypes.Bool;
import org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes;
import org.fisco.bcos.sdk.v3.codec.datatypes.Event;
import org.fisco.bcos.sdk.v3.codec.datatypes.Function;
import org.fisco.bcos.sdk.v3.codec.datatypes.Type;
import org.fisco.bcos.sdk.v3.codec.datatypes.TypeReference;
import org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple1;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple3;
import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.eventsub.EventSubCallback;
import org.fisco.bcos.sdk.v3.model.CryptoType;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.model.callback.TransactionCallback;
import org.fisco.bcos.sdk.v3.transaction.manager.transactionv1.ProxySignTransactionManager;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;

@SuppressWarnings("unchecked")
public class AppContract extends Contract {
    public static final String[] BINARY_ARRAY = {"608060405234801561001057600080fd5b50600080546001600160a01b031916339081178255604051909182917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0908290a350610ac6806100616000396000f3fe608060405234801561001057600080fd5b50600436106100a95760003560e01c80639670efcb116100715780639670efcb1461012c578063c09b261b1461013f578063c1cecc5a14610152578063f2fde38b14610165578063f76f703b14610178578063ff098be71461018b57600080fd5b80630a9d793d146100ae578063387868ae146100c35780633fecfe3f146100f3578063715018a6146101135780638da5cb5b1461011b575b600080fd5b6100c16100bc3660046107d6565b61019e565b005b6003546100d6906001600160a01b031681565b6040516001600160a01b0390911681526020015b60405180910390f35b610106610101366004610806565b6101f3565b6040516100ea9190610875565b6100c16102ac565b6000546001600160a01b03166100d6565b61010661013a366004610806565b610320565b6100c161014d366004610914565b61033c565b6100c1610160366004610914565b610400565b6100c16101733660046107d6565b6104ce565b6100c1610186366004610914565b6105b8565b6100c1610199366004610914565b610686565b6000546001600160a01b031633146101d15760405162461bcd60e51b81526004016101c8906109ac565b60405180910390fd5b600380546001600160a01b0319166001600160a01b0392909216919091179055565b6002602052816000526040600020818154811061020f57600080fd5b9060005260206000200160009150915050805461022b906109e1565b80601f0160208091040260200160405190810160405280929190818152602001828054610257906109e1565b80156102a45780601f10610279576101008083540402835291602001916102a4565b820191906000526020600020905b81548152906001019060200180831161028757829003601f168201915b505050505081565b6000546001600160a01b031633146102d65760405162461bcd60e51b81526004016101c8906109ac565b600080546040516001600160a01b03909116907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0908390a3600080546001600160a01b0319169055565b6001602052816000526040600020818154811061020f57600080fd5b6003546001600160a01b0316331461038b5760405162461bcd60e51b815260206004820152601260248201527124a72b20a624a22fa822a926a4a9a9a4a7a760711b60448201526064016101c8565b600082815260016020818152604083208054928301815583529182902083516103bc9391909201919084019061073d565b507f7b59e766dd02d6ce4e574f0ab75dfc4b180c90deb50dd9dbdbc65768abcf80f183838360016040516103f39493929190610a1c565b60405180910390a1505050565b6003546040516360e7662d60e11b81526001600160a01b039091169063c1cecc5a9061043490869086908690600401610a5b565b600060405180830381600087803b15801561044e57600080fd5b505af1158015610462573d6000803e3d6000fd5b50505060008381526002602090815260408220805460018101825590835291819020845161049794509201919084019061073d565b507fbbe83d8459c305cf51f2425b93e693dcfaaf60f22766486b0983c905b98ead4083838360006040516103f39493929190610a1c565b6000546001600160a01b031633146104f85760405162461bcd60e51b81526004016101c8906109ac565b6001600160a01b03811661055d5760405162461bcd60e51b815260206004820152602660248201527f4f776e61626c653a206e6577206f776e657220697320746865207a65726f206160448201526564647265737360d01b60648201526084016101c8565b600080546040516001600160a01b03808516939216917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e091a3600080546001600160a01b0319166001600160a01b0392909216919091179055565b60035460405163f76f703b60e01b81526001600160a01b039091169063f76f703b906105ec90869086908690600401610a5b565b600060405180830381600087803b15801561060657600080fd5b505af115801561061a573d6000803e3d6000fd5b50505060008381526002602090815260408220805460018101825590835291819020845161064f94509201919084019061073d565b507fbbe83d8459c305cf51f2425b93e693dcfaaf60f22766486b0983c905b98ead4083838360016040516103f39493929190610a1c565b6003546001600160a01b031633146106d55760405162461bcd60e51b815260206004820152601260248201527124a72b20a624a22fa822a926a4a9a9a4a7a760711b60448201526064016101c8565b600082815260016020818152604083208054928301815583529182902083516107069391909201919084019061073d565b507f7b59e766dd02d6ce4e574f0ab75dfc4b180c90deb50dd9dbdbc65768abcf80f183838360006040516103f39493929190610a1c565b828054610749906109e1565b90600052602060002090601f01602090048101928261076b57600085556107b1565b82601f1061078457805160ff19168380011785556107b1565b828001600101855582156107b1579182015b828111156107b1578251825591602001919060010190610796565b506107bd9291506107c1565b5090565b5b808211156107bd57600081556001016107c2565b6000602082840312156107e857600080fd5b81356001600160a01b03811681146107ff57600080fd5b9392505050565b6000806040838503121561081957600080fd5b50508035926020909101359150565b6000815180845260005b8181101561084e57602081850181015186830182015201610832565b81811115610860576000602083870101525b50601f01601f19169290920160200192915050565b6020815260006107ff6020830184610828565b634e487b7160e01b600052604160045260246000fd5b600067ffffffffffffffff808411156108b9576108b9610888565b604051601f8501601f19908116603f011681019082821181831017156108e1576108e1610888565b816040528093508581528686860111156108fa57600080fd5b858560208301376000602087830101525050509392505050565b60008060006060848603121561092957600080fd5b833567ffffffffffffffff8082111561094157600080fd5b818601915086601f83011261095557600080fd5b6109648783356020850161089e565b945060208601359350604086013591508082111561098157600080fd5b508401601f8101861361099357600080fd5b6109a28682356020840161089e565b9150509250925092565b6020808252818101527f4f776e61626c653a2063616c6c6572206973206e6f7420746865206f776e6572604082015260600190565b600181811c908216806109f557607f821691505b60208210811415610a1657634e487b7160e01b600052602260045260246000fd5b50919050565b608081526000610a2f6080830187610828565b8560208401528281036040840152610a478186610828565b915050821515606083015295945050505050565b606081526000610a6e6060830186610828565b8460208401528281036040840152610a868185610828565b969550505050505056fea2646970667358221220b93563937ef07a7caccdff5220b2ab6e8d910d8581199aaa786df19c2ce79c6664736f6c634300080b0033"};

    public static final String BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", BINARY_ARRAY);

    public static final String[] SM_BINARY_ARRAY = {"608060405234801561001057600080fd5b50600080546001600160a01b031916339081178255604051909182917f5c7c30d4a0f08950cb23be4132957b357fa5dfdb0fcf218f81b86a1c036e47d0908290a350610acc806100616000396000f3fe608060405234801561001057600080fd5b50600436106100a95760003560e01c80635089e2c8116100715780635089e2c81461012557806365699d8d1461014a57806390b286861461015d578063d84ecd6914610170578063d86e29e214610183578063e5b813ad1461018b57600080fd5b80630204ebc2146100ae578063084e7508146100c357806316cad12a146100ec5780632097809b146100ff578063281cf5e614610112575b600080fd5b6100c16100bc366004610868565b61019e565b005b6100d66100d1366004610900565b610279565b6040516100e3919061096f565b60405180910390f35b6100c16100fa366004610989565b610332565b6100c161010d366004610989565b610427565b6100c1610120366004610868565b610474565b6000546001600160a01b03165b6040516001600160a01b0390911681526020016100e3565b6100c1610158366004610868565b61052c565b6100d661016b366004610900565b6105fa565b600354610132906001600160a01b031681565b6100c1610616565b6100c1610199366004610868565b61068b565b60035460405163010275e160e11b81526001600160a01b0390911690630204ebc2906101d2908690869086906004016109b2565b600060405180830381600087803b1580156101ec57600080fd5b505af1158015610200573d6000803e3d6000fd5b505050600083815260026020908152604082208054600181018255908352918190208451610235945092019190840190610743565b507f200ed2971aa003016354f781cf280ada45672de3dc8c36583854d0bfe8b33a60838383600060405161026c94939291906109e7565b60405180910390a1505050565b6002602052816000526040600020818154811061029557600080fd5b906000526020600020016000915091505080546102b190610a26565b80601f01602080910402602001604051908101604052809291908181526020018280546102dd90610a26565b801561032a5780601f106102ff5761010080835404028352916020019161032a565b820191906000526020600020905b81548152906001019060200180831161030d57829003601f168201915b505050505081565b6000546001600160a01b0316331461036657604051636381e58960e11b815260040161035d90610a61565b60405180910390fd5b6001600160a01b0381166103cc57604051636381e58960e11b815260206004820152602660248201527f4f776e61626c653a206e6577206f776e657220697320746865207a65726f206160448201526564647265737360d01b606482015260840161035d565b600080546040516001600160a01b03808516939216917f5c7c30d4a0f08950cb23be4132957b357fa5dfdb0fcf218f81b86a1c036e47d091a3600080546001600160a01b0319166001600160a01b0392909216919091179055565b6000546001600160a01b0316331461045257604051636381e58960e11b815260040161035d90610a61565b600380546001600160a01b0319166001600160a01b0392909216919091179055565b6003546001600160a01b031633146104c457604051636381e58960e11b815260206004820152601260248201527124a72b20a624a22fa822a926a4a9a9a4a7a760711b604482015260640161035d565b600082815260016020818152604083208054928301815583529182902083516104f593919092019190840190610743565b507f96dd7a715fb188abef0d5ab36bd03d64c6457c75f997607aa73f9c3d85700ba5838383600160405161026c94939291906109e7565b6003546040516365699d8d60e01b81526001600160a01b03909116906365699d8d90610560908690869086906004016109b2565b600060405180830381600087803b15801561057a57600080fd5b505af115801561058e573d6000803e3d6000fd5b5050506000838152600260209081526040822080546001810182559083529181902084516105c3945092019190840190610743565b507f200ed2971aa003016354f781cf280ada45672de3dc8c36583854d0bfe8b33a60838383600160405161026c94939291906109e7565b6001602052816000526040600020818154811061029557600080fd5b6000546001600160a01b0316331461064157604051636381e58960e11b815260040161035d90610a61565b600080546040516001600160a01b03909116907f5c7c30d4a0f08950cb23be4132957b357fa5dfdb0fcf218f81b86a1c036e47d0908390a3600080546001600160a01b0319169055565b6003546001600160a01b031633146106db57604051636381e58960e11b815260206004820152601260248201527124a72b20a624a22fa822a926a4a9a9a4a7a760711b604482015260640161035d565b6000828152600160208181526040832080549283018155835291829020835161070c93919092019190840190610743565b507f96dd7a715fb188abef0d5ab36bd03d64c6457c75f997607aa73f9c3d85700ba5838383600060405161026c94939291906109e7565b82805461074f90610a26565b90600052602060002090601f01602090048101928261077157600085556107b7565b82601f1061078a57805160ff19168380011785556107b7565b828001600101855582156107b7579182015b828111156107b757825182559160200191906001019061079c565b506107c39291506107c7565b5090565b5b808211156107c357600081556001016107c8565b63b95aa35560e01b600052604160045260246000fd5b600067ffffffffffffffff8084111561080d5761080d6107dc565b604051601f8501601f19908116603f01168101908282118183101715610835576108356107dc565b8160405280935085815286868601111561084e57600080fd5b858560208301376000602087830101525050509392505050565b60008060006060848603121561087d57600080fd5b833567ffffffffffffffff8082111561089557600080fd5b818601915086601f8301126108a957600080fd5b6108b8878335602085016107f2565b94506020860135935060408601359150808211156108d557600080fd5b508401601f810186136108e757600080fd5b6108f6868235602084016107f2565b9150509250925092565b6000806040838503121561091357600080fd5b50508035926020909101359150565b6000815180845260005b818110156109485760208185018101518683018201520161092c565b8181111561095a576000602083870101525b50601f01601f19169290920160200192915050565b6020815260006109826020830184610922565b9392505050565b60006020828403121561099b57600080fd5b81356001600160a01b038116811461098257600080fd5b6060815260006109c56060830186610922565b84602084015282810360408401526109dd8185610922565b9695505050505050565b6080815260006109fa6080830187610922565b8560208401528281036040840152610a128186610922565b915050821515606083015295945050505050565b600181811c90821680610a3a57607f821691505b60208210811415610a5b5763b95aa35560e01b600052602260045260246000fd5b50919050565b6020808252818101527f4f776e61626c653a2063616c6c6572206973206e6f7420746865206f776e657260408201526060019056fea2646970667358221220468c71236df2ffaa311dfeb4c4a9a8d8876542b83f4ca53411b2ed3c9660a0d464736f6c634300080b0033"};

    public static final String SM_BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", SM_BINARY_ARRAY);

    public static final String[] ABI_ARRAY = {"[{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"address\",\"name\":\"previousOwner\",\"type\":\"address\"},{\"indexed\":true,\"internalType\":\"address\",\"name\":\"newOwner\",\"type\":\"address\"}],\"name\":\"OwnershipTransferred\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"internalType\":\"string\",\"name\":\"senderDomain\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"bytes32\",\"name\":\"author\",\"type\":\"bytes32\"},{\"indexed\":false,\"internalType\":\"bytes\",\"name\":\"message\",\"type\":\"bytes\"},{\"indexed\":false,\"internalType\":\"bool\",\"name\":\"isOrdered\",\"type\":\"bool\"}],\"name\":\"recvCrosschainMsg\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"internalType\":\"string\",\"name\":\"receiverDomain\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"bytes32\",\"name\":\"receiver\",\"type\":\"bytes32\"},{\"indexed\":false,\"internalType\":\"bytes\",\"name\":\"message\",\"type\":\"bytes\"},{\"indexed\":false,\"internalType\":\"bool\",\"name\":\"isOrdered\",\"type\":\"bool\"}],\"name\":\"sendCrosschainMsg\",\"type\":\"event\"},{\"conflictFields\":[{\"kind\":4,\"value\":[0]}],\"inputs\":[],\"name\":\"owner\",\"outputs\":[{\"internalType\":\"address\",\"name\":\"\",\"type\":\"address\"}],\"selector\":[2376452955,1351213768],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":3,\"slot\":1,\"value\":[1]},{\"kind\":4,\"value\":[3]}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"senderDomain\",\"type\":\"string\"},{\"internalType\":\"bytes32\",\"name\":\"author\",\"type\":\"bytes32\"},{\"internalType\":\"bytes\",\"name\":\"message\",\"type\":\"bytes\"}],\"name\":\"recvMessage\",\"outputs\":[],\"selector\":[3231393307,672986598],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0},{\"kind\":3,\"slot\":1,\"value\":[0]},{\"kind\":3,\"slot\":1,\"value\":[1]}],\"inputs\":[{\"internalType\":\"bytes32\",\"name\":\"\",\"type\":\"bytes32\"},{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"recvMsg\",\"outputs\":[{\"internalType\":\"bytes\",\"name\":\"\",\"type\":\"bytes\"}],\"selector\":[2523983819,2427618950],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":3,\"slot\":1,\"value\":[1]},{\"kind\":4,\"value\":[3]}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"senderDomain\",\"type\":\"string\"},{\"internalType\":\"bytes32\",\"name\":\"author\",\"type\":\"bytes32\"},{\"internalType\":\"bytes\",\"name\":\"message\",\"type\":\"bytes\"}],\"name\":\"recvUnorderedMessage\",\"outputs\":[],\"selector\":[4278815719,3854046125],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":4,\"value\":[0]}],\"inputs\":[],\"name\":\"renounceOwnership\",\"outputs\":[],\"selector\":[1901074598,3631098338],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":4,\"value\":[3]}],\"inputs\":[],\"name\":\"sdpAddress\",\"outputs\":[{\"internalType\":\"address\",\"name\":\"\",\"type\":\"address\"}],\"selector\":[947415214,3629043049],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"receiverDomain\",\"type\":\"string\"},{\"internalType\":\"bytes32\",\"name\":\"receiver\",\"type\":\"bytes32\"},{\"internalType\":\"bytes\",\"name\":\"message\",\"type\":\"bytes\"}],\"name\":\"sendMessage\",\"outputs\":[],\"selector\":[4151275579,1701420429],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0},{\"kind\":3,\"slot\":2,\"value\":[0]},{\"kind\":3,\"slot\":2,\"value\":[1]}],\"inputs\":[{\"internalType\":\"bytes32\",\"name\":\"\",\"type\":\"bytes32\"},{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"sendMsg\",\"outputs\":[{\"internalType\":\"bytes\",\"name\":\"\",\"type\":\"bytes\"}],\"selector\":[1072496191,139359496],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"receiverDomain\",\"type\":\"string\"},{\"internalType\":\"bytes32\",\"name\":\"receiver\",\"type\":\"bytes32\"},{\"internalType\":\"bytes\",\"name\":\"message\",\"type\":\"bytes\"}],\"name\":\"sendUnorderedMessage\",\"outputs\":[],\"selector\":[3251555418,33876930],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":4,\"value\":[0]},{\"kind\":4,\"value\":[3]}],\"inputs\":[{\"internalType\":\"address\",\"name\":\"protocolAddress\",\"type\":\"address\"}],\"name\":\"setProtocol\",\"outputs\":[],\"selector\":[178092349,546799771],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":4,\"value\":[0]}],\"inputs\":[{\"internalType\":\"address\",\"name\":\"newOwner\",\"type\":\"address\"}],\"name\":\"transferOwnership\",\"outputs\":[],\"selector\":[4076725131,382390570],\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]"};

    public static final String ABI = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", ABI_ARRAY);

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_RECVMESSAGE = "recvMessage";

    public static final String FUNC_RECVMSG = "recvMsg";

    public static final String FUNC_RECVUNORDEREDMESSAGE = "recvUnorderedMessage";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_SDPADDRESS = "sdpAddress";

    public static final String FUNC_SENDMESSAGE = "sendMessage";

    public static final String FUNC_SENDMSG = "sendMsg";

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

    protected AppContract(String contractAddress, Client client, CryptoKeyPair credential) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, credential);
        this.transactionManager = new ProxySignTransactionManager(client);
    }

    public static String getBinary(CryptoSuite cryptoSuite) {
        return (cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE ? BINARY : SM_BINARY);
    }

    public static String getABI() {
        return ABI;
    }

    public List<OwnershipTransferredEventResponse> getOwnershipTransferredEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, transactionReceipt);
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

    public void subscribeOwnershipTransferredEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(OWNERSHIPTRANSFERRED_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeOwnershipTransferredEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(OWNERSHIPTRANSFERRED_EVENT);
        subscribeEvent(topic0,callback);
    }

    public List<RecvCrosschainMsgEventResponse> getRecvCrosschainMsgEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(RECVCROSSCHAINMSG_EVENT, transactionReceipt);
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

    public void subscribeRecvCrosschainMsgEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(RECVCROSSCHAINMSG_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeRecvCrosschainMsgEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(RECVCROSSCHAINMSG_EVENT);
        subscribeEvent(topic0,callback);
    }

    public List<SendCrosschainMsgEventResponse> getSendCrosschainMsgEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SENDCROSSCHAINMSG_EVENT, transactionReceipt);
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

    public void subscribeSendCrosschainMsgEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(SENDCROSSCHAINMSG_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeSendCrosschainMsgEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(SENDCROSSCHAINMSG_EVENT);
        subscribeEvent(topic0,callback);
    }

    public String owner() throws ContractException {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeCallWithSingleValueReturn(function, String.class);
    }

    public Function getMethodOwnerRawFunction() throws ContractException {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return function;
    }

    public TransactionReceipt recvMessage(String senderDomain, byte[] author, byte[] message) {
        final Function function = new Function(
                FUNC_RECVMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(senderDomain), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(author), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodRecvMessageRawFunction(String senderDomain, byte[] author,
            byte[] message) throws ContractException {
        final Function function = new Function(FUNC_RECVMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(senderDomain), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(author), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(message)), 
                Arrays.<TypeReference<?>>asList());
        return function;
    }

    public String getSignedTransactionForRecvMessage(String senderDomain, byte[] author,
            byte[] message) {
        final Function function = new Function(
                FUNC_RECVMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(senderDomain), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(author), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    public String recvMessage(String senderDomain, byte[] author, byte[] message,
            TransactionCallback callback) {
        final Function function = new Function(
                FUNC_RECVMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(senderDomain), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(author), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple3<String, byte[], byte[]> getRecvMessageInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_RECVMESSAGE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}, new TypeReference<DynamicBytes>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<String, byte[], byte[]>(

                (String) results.get(0).getValue(), 
                (byte[]) results.get(1).getValue(), 
                (byte[]) results.get(2).getValue()
                );
    }

    public byte[] recvMsg(byte[] param0, BigInteger param1) throws ContractException {
        final Function function = new Function(FUNC_RECVMSG, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(param0), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
        return executeCallWithSingleValueReturn(function, byte[].class);
    }

    public Function getMethodRecvMsgRawFunction(byte[] param0, BigInteger param1) throws
            ContractException {
        final Function function = new Function(FUNC_RECVMSG, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(param0), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
        return function;
    }

    public TransactionReceipt recvUnorderedMessage(String senderDomain, byte[] author,
            byte[] message) {
        final Function function = new Function(
                FUNC_RECVUNORDEREDMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(senderDomain), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(author), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodRecvUnorderedMessageRawFunction(String senderDomain, byte[] author,
            byte[] message) throws ContractException {
        final Function function = new Function(FUNC_RECVUNORDEREDMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(senderDomain), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(author), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(message)), 
                Arrays.<TypeReference<?>>asList());
        return function;
    }

    public String getSignedTransactionForRecvUnorderedMessage(String senderDomain, byte[] author,
            byte[] message) {
        final Function function = new Function(
                FUNC_RECVUNORDEREDMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(senderDomain), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(author), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    public String recvUnorderedMessage(String senderDomain, byte[] author, byte[] message,
            TransactionCallback callback) {
        final Function function = new Function(
                FUNC_RECVUNORDEREDMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(senderDomain), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(author), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple3<String, byte[], byte[]> getRecvUnorderedMessageInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_RECVUNORDEREDMESSAGE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}, new TypeReference<DynamicBytes>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<String, byte[], byte[]>(

                (String) results.get(0).getValue(), 
                (byte[]) results.get(1).getValue(), 
                (byte[]) results.get(2).getValue()
                );
    }

    public TransactionReceipt renounceOwnership() {
        final Function function = new Function(
                FUNC_RENOUNCEOWNERSHIP, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodRenounceOwnershipRawFunction() throws ContractException {
        final Function function = new Function(FUNC_RENOUNCEOWNERSHIP, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList());
        return function;
    }

    public String getSignedTransactionForRenounceOwnership() {
        final Function function = new Function(
                FUNC_RENOUNCEOWNERSHIP, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    public String renounceOwnership(TransactionCallback callback) {
        final Function function = new Function(
                FUNC_RENOUNCEOWNERSHIP, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public String sdpAddress() throws ContractException {
        final Function function = new Function(FUNC_SDPADDRESS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeCallWithSingleValueReturn(function, String.class);
    }

    public Function getMethodSdpAddressRawFunction() throws ContractException {
        final Function function = new Function(FUNC_SDPADDRESS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return function;
    }

    public TransactionReceipt sendMessage(String receiverDomain, byte[] receiver, byte[] message) {
        final Function function = new Function(
                FUNC_SENDMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiverDomain), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(receiver), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodSendMessageRawFunction(String receiverDomain, byte[] receiver,
            byte[] message) throws ContractException {
        final Function function = new Function(FUNC_SENDMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiverDomain), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(receiver), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(message)), 
                Arrays.<TypeReference<?>>asList());
        return function;
    }

    public String getSignedTransactionForSendMessage(String receiverDomain, byte[] receiver,
            byte[] message) {
        final Function function = new Function(
                FUNC_SENDMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiverDomain), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(receiver), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    public String sendMessage(String receiverDomain, byte[] receiver, byte[] message,
            TransactionCallback callback) {
        final Function function = new Function(
                FUNC_SENDMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiverDomain), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(receiver), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple3<String, byte[], byte[]> getSendMessageInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_SENDMESSAGE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}, new TypeReference<DynamicBytes>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<String, byte[], byte[]>(

                (String) results.get(0).getValue(), 
                (byte[]) results.get(1).getValue(), 
                (byte[]) results.get(2).getValue()
                );
    }

    public byte[] sendMsg(byte[] param0, BigInteger param1) throws ContractException {
        final Function function = new Function(FUNC_SENDMSG, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(param0), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
        return executeCallWithSingleValueReturn(function, byte[].class);
    }

    public Function getMethodSendMsgRawFunction(byte[] param0, BigInteger param1) throws
            ContractException {
        final Function function = new Function(FUNC_SENDMSG, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(param0), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
        return function;
    }

    public TransactionReceipt sendUnorderedMessage(String receiverDomain, byte[] receiver,
            byte[] message) {
        final Function function = new Function(
                FUNC_SENDUNORDEREDMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiverDomain), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(receiver), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodSendUnorderedMessageRawFunction(String receiverDomain, byte[] receiver,
            byte[] message) throws ContractException {
        final Function function = new Function(FUNC_SENDUNORDEREDMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiverDomain), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(receiver), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(message)), 
                Arrays.<TypeReference<?>>asList());
        return function;
    }

    public String getSignedTransactionForSendUnorderedMessage(String receiverDomain,
            byte[] receiver, byte[] message) {
        final Function function = new Function(
                FUNC_SENDUNORDEREDMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiverDomain), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(receiver), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    public String sendUnorderedMessage(String receiverDomain, byte[] receiver, byte[] message,
            TransactionCallback callback) {
        final Function function = new Function(
                FUNC_SENDUNORDEREDMESSAGE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiverDomain), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(receiver), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes(message)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple3<String, byte[], byte[]> getSendUnorderedMessageInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_SENDUNORDEREDMESSAGE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}, new TypeReference<DynamicBytes>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<String, byte[], byte[]>(

                (String) results.get(0).getValue(), 
                (byte[]) results.get(1).getValue(), 
                (byte[]) results.get(2).getValue()
                );
    }

    public TransactionReceipt setProtocol(String protocolAddress) {
        final Function function = new Function(
                FUNC_SETPROTOCOL, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Address(protocolAddress)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodSetProtocolRawFunction(String protocolAddress) throws
            ContractException {
        final Function function = new Function(FUNC_SETPROTOCOL, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Address(protocolAddress)), 
                Arrays.<TypeReference<?>>asList());
        return function;
    }

    public String getSignedTransactionForSetProtocol(String protocolAddress) {
        final Function function = new Function(
                FUNC_SETPROTOCOL, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Address(protocolAddress)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    public String setProtocol(String protocolAddress, TransactionCallback callback) {
        final Function function = new Function(
                FUNC_SETPROTOCOL, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Address(protocolAddress)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple1<String> getSetProtocolInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_SETPROTOCOL, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<String>(

                (String) results.get(0).getValue()
                );
    }

    public TransactionReceipt transferOwnership(String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Address(newOwner)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodTransferOwnershipRawFunction(String newOwner) throws
            ContractException {
        final Function function = new Function(FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Address(newOwner)), 
                Arrays.<TypeReference<?>>asList());
        return function;
    }

    public String getSignedTransactionForTransferOwnership(String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Address(newOwner)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    public String transferOwnership(String newOwner, TransactionCallback callback) {
        final Function function = new Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Address(newOwner)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple1<String> getTransferOwnershipInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        final Function function = new Function(FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<String>(

                (String) results.get(0).getValue()
                );
    }

    public static AppContract load(String contractAddress, Client client,
            CryptoKeyPair credential) {
        return new AppContract(contractAddress, client, credential);
    }

    public static AppContract deploy(Client client, CryptoKeyPair credential) throws
            ContractException {
        return deploy(AppContract.class, client, credential, getBinary(client.getCryptoSuite()), getABI(), null, null);
    }

    public static class OwnershipTransferredEventResponse {
        public TransactionReceipt.Logs log;

        public String previousOwner;

        public String newOwner;
    }

    public static class RecvCrosschainMsgEventResponse {
        public TransactionReceipt.Logs log;

        public String senderDomain;

        public byte[] author;

        public byte[] message;

        public Boolean isOrdered;
    }

    public static class SendCrosschainMsgEventResponse {
        public TransactionReceipt.Logs log;

        public String receiverDomain;

        public byte[] receiver;

        public byte[] message;

        public Boolean isOrdered;
    }
}
