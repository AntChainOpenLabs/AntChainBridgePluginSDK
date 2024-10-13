package com.alipay.antchain.bridge.abi;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

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
public class AuthMsg extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b50600080546001600160a01b031916339081178255604051909182917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0908290a350600180546001600160a01b0319163317905561218c806100736000396000f3fe608060405234801561001057600080fd5b50600436106100a95760003560e01c80638406c079116100715780638406c0791461018f5780638da5cb5b146101a2578063a7566e77146101b3578063e41fb517146101c6578063e85df5f7146101d9578063f2fde38b146101ec57600080fd5b8063189a0a15146100ae5780636548e9bc146100fa578063705c572c1461010f578063715018a614610138578063747c7ffd14610140575b600080fd5b6100dd6100bc366004611cd6565b63ffffffff166000908152600360205260409020546001600160a01b031690565b6040516001600160a01b0390911681526020015b60405180910390f35b61010d610108366004611d08565b6101ff565b005b6100dd61011d366004611cd6565b6003602052600090815260409020546001600160a01b031681565b61010d610254565b61017361014e366004611d08565b60026020526000908152604090205463ffffffff811690640100000000900460ff1682565b6040805163ffffffff90931683529015156020830152016100f1565b6001546100dd906001600160a01b031681565b6000546001600160a01b03166100dd565b61010d6101c1366004611dc6565b6102c8565b61010d6101d4366004611dfb565b61038a565b61010d6101e7366004611e49565b61048d565b61010d6101fa366004611d08565b6105ab565b6000546001600160a01b031633146102325760405162461bcd60e51b815260040161022990611e7c565b60405180910390fd5b600180546001600160a01b0319166001600160a01b0392909216919091179055565b6000546001600160a01b0316331461027e5760405162461bcd60e51b815260040161022990611e7c565b600080546040516001600160a01b03909116907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0908390a3600080546001600160a01b0319169055565b6001546001600160a01b0316331461032c5760405162461bcd60e51b815260206004820152602160248201527f417574684d73673a2073656e646572206e6f742076616c69642072656c6179656044820152603960f91b6064820152608401610229565b60008061033883610695565b915091507f2b41cea8d63514764033a161efa4273751d3f20eda0ecd3c514fa43b99f38bb0828260405161036d929190611efe565b60405180910390a1610385610382838361076e565b50565b505050565b33600090815260026020526040902054640100000000900460ff166104005760405162461bcd60e51b815260206004820152602660248201527f417574684d73673a2073656e646572206e6f742076616c6964207375622d70726044820152651bdd1bd8dbdb60d21b6064820152608401610229565b60006040518060800160405280600163ffffffff1681526020016104238561089a565b8152336000908152600260209081526040918290205463ffffffff16908301520183905290507f79b7516b1b7a6a39fb4b7b22e8667cd3744e5c27425292f8a9f49d1042c0c651610473826108ad565b6040516104809190611f23565b60405180910390a1505050565b6000546001600160a01b031633146104b75760405162461bcd60e51b815260040161022990611e7c565b6001600160a01b03821660009081526002602052604090208054640100000000900460ff16156105295760405162461bcd60e51b815260206004820152601860248201527f417574684d73673a2070726f746f636f6c2065786973747300000000000000006044820152606401610229565b805463ffffffff831664ffffffffff19909116811764010000000017825560008181526003602090815260409182902080546001600160a01b0388166001600160a01b0319909116811790915591519182527f1d5c04c569492046e8119073d290445582589ec4f52031b868ed74b2b7f7bfa8910160405180910390a2505050565b6000546001600160a01b031633146105d55760405162461bcd60e51b815260040161022990611e7c565b6001600160a01b03811661063a5760405162461bcd60e51b815260206004820152602660248201527f4f776e61626c653a206e6577206f776e657220697320746865207a65726f206160448201526564647265737360d01b6064820152608401610229565b600080546040516001600160a01b03808516939216917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e091a3600080546001600160a01b0319166001600160a01b0392909216919091179055565b6060806000806106a782866004610941565b90506106b4600483611f4c565b915060006106c3600483015190565b905060006106d884888463ffffffff16610941565b90506106ea63ffffffff831685611f4c565b935060006106fa85896004610941565b9050610707600486611f4c565b94506000610716600483015190565b9050600061072b878b8463ffffffff16610941565b905061073d63ffffffff831688611f4c565b604080518082019091528581526020810183905290975061075d82610956565b995099505050505050505050915091565b604080516080810182526000808252602082018190529181018290526060808201529061079a83610986565b60408082015163ffffffff166000908152600360205220549091506001600160a01b031661080a5760405162461bcd60e51b815260206004820152601a60248201527f417574684d73673a206e6f2070726f746f636f6c2065786973740000000000006044820152606401610229565b60408082015163ffffffff166000908152600360209081529082902054908301516060840151925163c09b261b60e01b81526001600160a01b039092169263c09b261b9261085e9289929091600401611f64565b600060405180830381600087803b15801561087857600080fd5b505af115801561088c573d6000803e3d6000fd5b509293505050505b92915050565b6000806108a683610a3e565b9392505050565b6060816000015163ffffffff16600114156108cb5761089482610a6c565b816000015163ffffffff16600214156108e75761089482610be0565b60405162461bcd60e51b815260206004820152602960248201527f656e636f6465417574684d6573736167653a20616d2076657273696f6e206e6f6044820152681d081cdd5c1c1bdc9d60ba1b6064820152608401610229565b606061094e848484610dac565b949350505050565b606080600061096484610e1b565b905060006109758260200151610fbb565b608090920151959194509092505050565b60408051608081018252600080825260208201819052918101829052606080820152906109b2836110b0565b90508063ffffffff16600114156109cc576108a6836110bd565b8063ffffffff16600214156109e4576108a6836111db565b60405162461bcd60e51b815260206004820152602960248201527f6465636f6465417574684d6573736167653a20616d2076657273696f6e206e6f6044820152681d081cdd5c1c1bdc9d60ba1b6064820152608401610229565b6040805160208082528183019092526000918291906020820181803683375050506020018390525090919050565b6060816000015163ffffffff16600114610ad35760405162461bcd60e51b815260206004820152602260248201527f656e636f6465417574684d65737361676556313a2077726f6e6720766572736960448201526137b760f11b6064820152608401610229565b6000610ae28360600151611296565b610aed906004611f4c565b610af8906020611f4c565b610b03906004611f4c565b905060008167ffffffffffffffff811115610b2057610b20611d23565b6040519080825280601f01601f191660200182016040528015610b4a576020820181803683370190505b5084519091508290610b5e908290846112e2565b610b6860206112f3565b610b729082611f99565b60208681015182850152909150610b899082611f99565b9050610b9a818660400151846112e2565b610ba460206112f3565b610bae9082611f99565b9050610bbf81866060015184611522565b610bcc8560600151611296565b610bd69082611f99565b5090949350505050565b6060816000015163ffffffff16600214610c475760405162461bcd60e51b815260206004820152602260248201527f656e636f6465417574684d65737361676556323a2077726f6e6720766572736960448201526137b760f11b6064820152608401610229565b63ffffffff8260600151511115610cb35760405162461bcd60e51b815260206004820152602a60248201527f656e636f6465417574684d65737361676556323a20626f6479206c656e677468604482015269081bdd995c9b1a5b5a5d60b21b6064820152608401610229565b60008260600151516004610cc79190611f4c565b610cd2906004611f4c565b610cdd906020611f4c565b610ce8906004611f4c565b905060008167ffffffffffffffff811115610d0557610d05611d23565b6040519080825280601f01601f191660200182016040528015610d2f576020820181803683370190505b5084519091508290610d43908290846112e2565b610d4d60206112f3565b610d579082611f99565b60208681015182850152909150610d6e9082611f99565b9050610d7f818660400151846112e2565b610d8960206112f3565b610d939082611f99565b9050610da481866060015184611587565b509392505050565b60608082158015610dc857604051915060208201604052610e12565b6040519150601f8416801560200281840101858101888315602002848a0101015b81831015610e01578051835260209283019201610de9565b5050858452601f01601f1916604052505b50949350505050565b6040805161010081018252606060c0820181815260e083018290528252602082018190526000928201839052808201819052608082015260a08101919091526040805161010081018252606060c0820181815260e083018290528252602082018190526000928201839052808201819052608082015260a081019190915260065b8351811015610fb45760408051606080820183526000808352602083015291810191909152610ecb8583611675565b815190935090915061ffff1660041415610ef357610eec81604001516117cc565b8352610fae565b805161ffff1660051415610f105760408101516020840152610fae565b805161ffff1660071415610f4557610f35610f3082604001516000611865565b61188f565b63ffffffff166040840152610fae565b805161ffff1660081415610f625760408101516060840152610fae565b805161ffff1660091415610f7f5760408101516080840152610fae565b805161ffff16600a1415610fae57610fa4610f9f82604001516000611992565b6119ba565b61ffff1660a08401525b50610e9c565b5092915050565b6060600c82511161100e5760405162461bcd60e51b815260206004820152601b60248201527f696c6c6567616c206c656e677468206f662075646167207265737000000000006044820152606401610229565b600061101e610f30846008611865565b905061102b81600c611fb0565b63ffffffff168351101561109e5760405162461bcd60e51b815260206004820152603460248201527f6c656e677468206f6620756461672072657370206c657373207468616e20746860448201527365206c656e677468206f66206d736720626f647960601b6064820152608401610229565b6108a6600c848363ffffffff16610dac565b6000610894825183015190565b604080516080810182526000808252602082018190529181019190915260608082015260006110ec60206112f3565b83516110f89190611f99565b905060006111068285015190565b9050611113602083611f99565b915060006111218386015190565b905061112d60206112f3565b6111379084611f99565b925060006111458487611a52565b67ffffffffffffffff81111561115d5761115d611d23565b6040519080825280601f01601f191660200182016040528015611187576020820181803683370190505b509050611195848783611a74565b61119e81611296565b6111a89085611f99565b506040805160808101825260018152602081019490945263ffffffff929092169183019190915260608201529392505050565b6040805160808101825260008082526020820181905291810191909152606080820152600061120a60206112f3565b83516112169190611f99565b905060006112248285015190565b9050611231602083611f99565b9150600061123f8386015190565b905061124b60206112f3565b6112559084611f99565b925060006112638487611abb565b6040805160808101825260028152602081019590955263ffffffff93909316928401929092525060608201529392505050565b6000602082516112a69190611fee565b9050602082516112b69190612002565b156112c957806112c581612016565b9150505b806112d381612016565b91506108949050602082612031565b909101600319810180519290915252565b6000816008811461140057601081146114095760188114611412576020811461141b5760288114611424576030811461142d5760388114611436576040811461143f57604881146114485760508114611451576058811461145a5760608114611463576068811461146c5760708114611475576078811461147e57608081146114875760888114611490576090811461149957609881146114a25760a081146114ab5760a881146114b45760b081146114bd5760b881146114c65760c081146114cf5760c881146114d85760d081146114e15760d881146114ea5760e081146114f35760e881146114fc5760f081146115055760f8811461150e576101008114611517576020915061151c565b6001915061151c565b6002915061151c565b6003915061151c565b6004915061151c565b6005915061151c565b6006915061151c565b6007915061151c565b6008915061151c565b6009915061151c565b600a915061151c565b600b915061151c565b600c915061151c565b600d915061151c565b600e915061151c565b600f915061151c565b6010915061151c565b6011915061151c565b6012915061151c565b6013915061151c565b6014915061151c565b6015915061151c565b6016915061151c565b6017915061151c565b6018915061151c565b6019915061151c565b601a915061151c565b601b915061151c565b601c915061151c565b601d915061151c565b601e915061151c565b601f915061151c565b602091505b50919050565b6000602083516115329190611fee565b90506000602084516115449190612002565b1115611558578061155481612016565b9150505b60010160005b81811015611580576020810284015183860152601f199094019360010161155e565b5050505050565b81516115948482846112e2565b61159f600485611f99565b93508063ffffffff168410156116125760405162461bcd60e51b815260206004820152603260248201527f7661724279746573546f42797465733a206f6666736574206c657373207468616044820152710dc40e8d0ca40d2dce0eae840d8cadccee8d60731b6064820152608401610229565b61162263ffffffff821685611f99565b93508015801561163157611580565b8483018051601f84168015602002818801018581018215602002838601015b81831015611668578251815260209283019201611650565b5050509152505050505050565b604080516060808201835260008083526020830152918101919091526000828451116116ef5760405162461bcd60e51b815260206004820152602360248201527f6c656e677468206f66207261772064617461206c657373207468616e206f66666044820152621cd95d60ea1b6064820152608401610229565b60068310156117315760405162461bcd60e51b815260206004820152600e60248201526d1a5b1b1959d85b081bd9999cd95d60921b6044820152606401610229565b6040805160608082018352600080835260208301529181019190915261175a610f9f8686611992565b61ffff16815261176b600285611f4c565b935061177a610f308686611865565b63ffffffff166020820152611790600485611f4c565b93506117a78585836020015163ffffffff16611bc0565b604082015260208101516117c19063ffffffff1685611f4c565b909590945092505050565b6040805180820190915260608082526020820152604080518082019091526060808252602082015260065b8351811015610fb457604080516060808201835260008083526020830152918101919091526118268583611675565b815190935090915061ffff1660011415611846576040810151835261185f565b805161ffff166002141561185f57604081015160208401525b506117f7565b8151600090611875836004611f4c565b111561188057600080fd5b50016004015163ffffffff1690565b60408051600480825281830190925260009160e084901b9183916020820181803683370190505090508160031a60f81b816000815181106118d2576118d2612050565b60200101906001600160f81b031916908160001a9053508160021a60f81b8160018151811061190357611903612050565b60200101906001600160f81b031916908160001a9053508160011a60f81b8160028151811061193457611934612050565b60200101906001600160f81b031916908160001a9053508160001a60f81b8160038151811061196557611965612050565b60200101906001600160f81b031916908160001a9053506000611989826000611865565b95945050505050565b81516000906119a2836002611f4c565b11156119ad57600080fd5b50016002015161ffff1690565b60408051600280825281830190925260009160f084901b9183916020820181803683370190505090508160011a60f81b816000815181106119fd576119fd612050565b60200101906001600160f81b031916908160001a9053508160001a60f81b81600181518110611a2e57611a2e612050565b60200101906001600160f81b031916908160001a9053506000611989826000611992565b8082015160208104600101601f821615611a6a576001015b6020029392505050565b81830151600060208204600101601f831615611a8e576001015b5b80821015611ab3578585015160208302850152602086039550600182019150611a8f565b505050505050565b60606000611ac98484015190565b63ffffffff169050611adc600485611f99565b935083811115611b475760405162461bcd60e51b815260206004820152603060248201527f6279746573546f56617242797465733a206f6666736574206c6573732074686160448201526f6e206c656e677468206f6620626f647960801b6064820152608401610229565b611b518185611f99565b9350606081158015611b6e57604051915060208201604052610e12565b6040519150601f8316801560200281840101848101888315602002848a0101015b81831015611ba7578051835260209283019201611b8f565b5050848452601f01601f19166040525050949350505050565b8251606090611bcf8385611f4c565b1115611bda57600080fd5b60008267ffffffffffffffff811115611bf557611bf5611d23565b6040519080825280601f01601f191660200182016040528015611c1f576020820181803683370190505b50905060208082019086860101611c37828287611c42565b509095945050505050565b60208110611c7a5781518352611c59602084611f4c565b9250611c66602083611f4c565b9150611c73602082611f99565b9050611c42565b80611c8457505050565b60006001611c93836020611f99565b611c9f9061010061214a565b611ca99190611f99565b925184518416931916929092179092525050565b803563ffffffff81168114611cd157600080fd5b919050565b600060208284031215611ce857600080fd5b6108a682611cbd565b80356001600160a01b0381168114611cd157600080fd5b600060208284031215611d1a57600080fd5b6108a682611cf1565b634e487b7160e01b600052604160045260246000fd5b600082601f830112611d4a57600080fd5b813567ffffffffffffffff80821115611d6557611d65611d23565b604051601f8301601f19908116603f01168101908282118183101715611d8d57611d8d611d23565b81604052838152866020858801011115611da657600080fd5b836020870160208301376000602085830101528094505050505092915050565b600060208284031215611dd857600080fd5b813567ffffffffffffffff811115611def57600080fd5b61094e84828501611d39565b60008060408385031215611e0e57600080fd5b611e1783611cf1565b9150602083013567ffffffffffffffff811115611e3357600080fd5b611e3f85828601611d39565b9150509250929050565b60008060408385031215611e5c57600080fd5b611e6583611cf1565b9150611e7360208401611cbd565b90509250929050565b6020808252818101527f4f776e61626c653a2063616c6c6572206973206e6f7420746865206f776e6572604082015260600190565b6000815180845260005b81811015611ed757602081850181015186830182015201611ebb565b81811115611ee9576000602083870101525b50601f01601f19169290920160200192915050565b604081526000611f116040830185611eb1565b82810360208401526119898185611eb1565b6020815260006108a66020830184611eb1565b634e487b7160e01b600052601160045260246000fd5b60008219821115611f5f57611f5f611f36565b500190565b606081526000611f776060830186611eb1565b8460208401528281036040840152611f8f8185611eb1565b9695505050505050565b600082821015611fab57611fab611f36565b500390565b600063ffffffff808316818516808303821115611fcf57611fcf611f36565b01949350505050565b634e487b7160e01b600052601260045260246000fd5b600082611ffd57611ffd611fd8565b500490565b60008261201157612011611fd8565b500690565b600060001982141561202a5761202a611f36565b5060010190565b600081600019048311821515161561204b5761204b611f36565b500290565b634e487b7160e01b600052603260045260246000fd5b600181815b808511156120a157816000190482111561208757612087611f36565b8085161561209457918102915b93841c939080029061206b565b509250929050565b6000826120b857506001610894565b816120c557506000610894565b81600181146120db57600281146120e557612101565b6001915050610894565b60ff8411156120f6576120f6611f36565b50506001821b610894565b5060208310610133831016604e8410600b8410161715612124575081810a610894565b61212e8383612066565b806000190482111561214257612142611f36565b029392505050565b60006108a683836120a956fea264697066735822122067e49773b2b6bc84d55e68facb2f2f2de126ca807208e3774622400944816c0664736f6c63430008090033";

    public static final String FUNC_GETPROTOCOL = "getProtocol";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_PROTOCOLROUTES = "protocolRoutes";

    public static final String FUNC_RECVFROMPROTOCOL = "recvFromProtocol";

    public static final String FUNC_RECVPKGFROMRELAYER = "recvPkgFromRelayer";

    public static final String FUNC_RELAYER = "relayer";

    public static final String FUNC_RENOUNCEOWNERSHIP = "renounceOwnership";

    public static final String FUNC_SETPROTOCOL = "setProtocol";

    public static final String FUNC_SETRELAYER = "setRelayer";

    public static final String FUNC_SUBPROTOCOLS = "subProtocols";

    public static final String FUNC_TRANSFEROWNERSHIP = "transferOwnership";

    public static final Event OWNERSHIPTRANSFERRED_EVENT = new Event("OwnershipTransferred",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event SENDAUTHMESSAGE_EVENT = new Event("SendAuthMessage",
            Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
    ;

    public static final Event SUBPROTOCOLUPDATE_EVENT = new Event("SubProtocolUpdate",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>(true) {}, new TypeReference<Address>() {}));
    ;

    public static final Event RECVAUTHMESSAGE_EVENT = new Event("recvAuthMessage",
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<DynamicBytes>() {}));
    ;

    @Deprecated
    protected AuthMsg(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected AuthMsg(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected AuthMsg(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected AuthMsg(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
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

    public static List<SendAuthMessageEventResponse> getSendAuthMessageEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SENDAUTHMESSAGE_EVENT, transactionReceipt);
        ArrayList<SendAuthMessageEventResponse> responses = new ArrayList<SendAuthMessageEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SendAuthMessageEventResponse typedResponse = new SendAuthMessageEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.pkg = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<SendAuthMessageEventResponse> sendAuthMessageEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, SendAuthMessageEventResponse>() {
            @Override
            public SendAuthMessageEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SENDAUTHMESSAGE_EVENT, log);
                SendAuthMessageEventResponse typedResponse = new SendAuthMessageEventResponse();
                typedResponse.log = log;
                typedResponse.pkg = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<SendAuthMessageEventResponse> sendAuthMessageEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SENDAUTHMESSAGE_EVENT));
        return sendAuthMessageEventFlowable(filter);
    }

    public static List<SubProtocolUpdateEventResponse> getSubProtocolUpdateEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SUBPROTOCOLUPDATE_EVENT, transactionReceipt);
        ArrayList<SubProtocolUpdateEventResponse> responses = new ArrayList<SubProtocolUpdateEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SubProtocolUpdateEventResponse typedResponse = new SubProtocolUpdateEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.protocolType = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.protocolAddress = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<SubProtocolUpdateEventResponse> subProtocolUpdateEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, SubProtocolUpdateEventResponse>() {
            @Override
            public SubProtocolUpdateEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SUBPROTOCOLUPDATE_EVENT, log);
                SubProtocolUpdateEventResponse typedResponse = new SubProtocolUpdateEventResponse();
                typedResponse.log = log;
                typedResponse.protocolType = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.protocolAddress = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<SubProtocolUpdateEventResponse> subProtocolUpdateEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SUBPROTOCOLUPDATE_EVENT));
        return subProtocolUpdateEventFlowable(filter);
    }

    public static List<RecvAuthMessageEventResponse> getRecvAuthMessageEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(RECVAUTHMESSAGE_EVENT, transactionReceipt);
        ArrayList<RecvAuthMessageEventResponse> responses = new ArrayList<RecvAuthMessageEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RecvAuthMessageEventResponse typedResponse = new RecvAuthMessageEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.recvDomain = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.rawMsg = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<RecvAuthMessageEventResponse> recvAuthMessageEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, RecvAuthMessageEventResponse>() {
            @Override
            public RecvAuthMessageEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(RECVAUTHMESSAGE_EVENT, log);
                RecvAuthMessageEventResponse typedResponse = new RecvAuthMessageEventResponse();
                typedResponse.log = log;
                typedResponse.recvDomain = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.rawMsg = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<RecvAuthMessageEventResponse> recvAuthMessageEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RECVAUTHMESSAGE_EVENT));
        return recvAuthMessageEventFlowable(filter);
    }

    public RemoteFunctionCall<String> getProtocol(BigInteger protocolType) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETPROTOCOL,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(protocolType)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> owner() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_OWNER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> protocolRoutes(BigInteger param0) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_PROTOCOLROUTES,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(param0)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> recvFromProtocol(String senderID, byte[] message) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_RECVFROMPROTOCOL,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, senderID),
                        new org.web3j.abi.datatypes.DynamicBytes(message)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> recvPkgFromRelayer(byte[] pkg) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_RECVPKGFROMRELAYER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicBytes(pkg)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> relayer() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_RELAYER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> renounceOwnership() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_RENOUNCEOWNERSHIP,
                Arrays.<Type>asList(),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setProtocol(String protocolAddress, BigInteger protocolType) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETPROTOCOL,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, protocolAddress),
                        new org.web3j.abi.datatypes.generated.Uint32(protocolType)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setRelayer(String relayerAddress) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETRELAYER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, relayerAddress)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Tuple2<BigInteger, Boolean>> subProtocols(String param0) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SUBPROTOCOLS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint32>() {}, new TypeReference<Bool>() {}));
        return new RemoteFunctionCall<Tuple2<BigInteger, Boolean>>(function,
                new Callable<Tuple2<BigInteger, Boolean>>() {
                    @Override
                    public Tuple2<BigInteger, Boolean> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<BigInteger, Boolean>(
                                (BigInteger) results.get(0).getValue(),
                                (Boolean) results.get(1).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> transferOwnership(String newOwner) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_TRANSFEROWNERSHIP,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, newOwner)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static AuthMsg load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new AuthMsg(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static AuthMsg load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new AuthMsg(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static AuthMsg load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new AuthMsg(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static AuthMsg load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new AuthMsg(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<AuthMsg> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(AuthMsg.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<AuthMsg> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(AuthMsg.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<AuthMsg> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(AuthMsg.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<AuthMsg> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(AuthMsg.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public String previousOwner;

        public String newOwner;
    }

    public static class SendAuthMessageEventResponse extends BaseEventResponse {
        public byte[] pkg;
    }

    public static class SubProtocolUpdateEventResponse extends BaseEventResponse {
        public BigInteger protocolType;

        public String protocolAddress;
    }

    public static class RecvAuthMessageEventResponse extends BaseEventResponse {
        public String recvDomain;

        public byte[] rawMsg;
    }
}
