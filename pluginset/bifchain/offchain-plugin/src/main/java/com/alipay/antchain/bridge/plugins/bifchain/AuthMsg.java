package com.alipay.antchain.bridge.plugins.bifchain;

import cn.bif.utils.generator.entity.Contract;
import cn.bif.utils.generator.entity.TypeReference;
import cn.bif.utils.generator.entity.datatypes.*;
import cn.bif.utils.generator.entity.datatypes.primitive.Long;
import cn.bif.utils.generator.response.BaseEventResponse;
import cn.bif.utils.generator.response.Log;

import java.util.Arrays;
import java.util.Collections;

public class AuthMsg extends Contract {
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


    public static final Event SENDAUTHMESSAGE_EVENT = new Event("SendAuthMessage", 
            Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));


    public static final Event SUBPROTOCOLUPDATE_EVENT = new Event("SubProtocolUpdate", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Long>(true) {}, new TypeReference<Address>() {}));


    public static final Event RECVAUTHMESSAGE_EVENT = new Event("recvAuthMessage", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<DynamicBytes>() {}));


    public static OwnershipTransferredEventResponse getOwnershipTransferredEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(OWNERSHIPTRANSFERRED_EVENT, log);
        OwnershipTransferredEventResponse typedResponse = new OwnershipTransferredEventResponse();
        typedResponse.event = "OwnershipTransferred(address indexed previousOwner,address indexed newOwner)";
        typedResponse.result = new OwnershipTransferredEventResponse.Result();
        typedResponse.log = log;
        typedResponse.result.previousOwner = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.result.newOwner = (String) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public static SendAuthMessageEventResponse getSendAuthMessageEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SENDAUTHMESSAGE_EVENT, log);
        SendAuthMessageEventResponse typedResponse = new SendAuthMessageEventResponse();
        typedResponse.event = "SendAuthMessage(bytes pkg)";
        typedResponse.result = new SendAuthMessageEventResponse.Result();
        typedResponse.log = log;
        typedResponse.result.pkg = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public static SubProtocolUpdateEventResponse getSubProtocolUpdateEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SUBPROTOCOLUPDATE_EVENT, log);
        SubProtocolUpdateEventResponse typedResponse = new SubProtocolUpdateEventResponse();
        typedResponse.event = "SubProtocolUpdate(uint32 indexed protocolType,address protocolAddress)";
        typedResponse.result = new SubProtocolUpdateEventResponse.Result();
        typedResponse.log = log;
        typedResponse.result.protocolType = (java.lang.Long) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.result.protocolAddress = Address.base58Ecode(eventValues.getNonIndexedValues().get(0).getValue());
        return typedResponse;
    }

    public static RecvAuthMessageEventResponse getRecvAuthMessageEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(RECVAUTHMESSAGE_EVENT, log);
        RecvAuthMessageEventResponse typedResponse = new RecvAuthMessageEventResponse();
        typedResponse.event = "recvAuthMessage(string recvDomain,bytes rawMsg)";
        typedResponse.result = new RecvAuthMessageEventResponse.Result();
        typedResponse.log = log;
        typedResponse.result.recvDomain = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.result.rawMsg = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Object getProtocol(String value) {
        final Function function = new Function(FUNC_GETPROTOCOL, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return hexadecimalDecode(value,function);
    }

    public Object owner(String value) {
        final Function function = new Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return hexadecimalDecode(value,function);
    }

    public Object protocolRoutes(String value) {
        final Function function = new Function(FUNC_PROTOCOLROUTES, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return hexadecimalDecode(value,function);
    }

    public String recvFromProtocol(String senderID, byte[] message) {
        final Function function = new Function(
                FUNC_RECVFROMPROTOCOL, 
                Arrays.<Type>asList(new Address(senderID),
                new DynamicBytes(message)),
                Collections.<TypeReference<?>>emptyList());
        return hexadecimalString(function);
    }

    public String recvPkgFromRelayer(byte[] pkg) {
        final Function function = new Function(
                FUNC_RECVPKGFROMRELAYER, 
                Arrays.<Type>asList(new DynamicBytes(pkg)),
                Collections.<TypeReference<?>>emptyList());
        return hexadecimalString(function);
    }

    public Object relayer(String value) {
        final Function function = new Function(FUNC_RELAYER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return hexadecimalDecode(value,function);
    }

    public String renounceOwnership() {
        final Function function = new Function(
                FUNC_RENOUNCEOWNERSHIP, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return hexadecimalString(function);
    }

    public String setProtocol(String protocolAddress, java.lang.Long protocolType) {
        final Function function = new Function(
                FUNC_SETPROTOCOL, 
                Arrays.<Type>asList(new Address(protocolAddress),
                new Long(protocolType)),
                Collections.<TypeReference<?>>emptyList());
        return hexadecimalString(function);
    }

    public String setRelayer(String relayerAddress) {
        final Function function = new Function(
                FUNC_SETRELAYER, 
                Arrays.<Type>asList(new Address(relayerAddress)),
                Collections.<TypeReference<?>>emptyList());
        return hexadecimalString(function);
    }

    public Object subProtocols(String value) {
        final Function function = new Function(FUNC_SUBPROTOCOLS, 
                Arrays.<Type>asList(new Utf8String(value)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Long>() {}, new TypeReference<Bool>() {}));
        return hexadecimalDecode(value,function);
    }

    public String transferOwnership(String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFEROWNERSHIP, 
                Arrays.<Type>asList(new Address(newOwner)),
                Collections.<TypeReference<?>>emptyList());
        return hexadecimalString(function);
    }

    public static class OwnershipTransferredEventResponse extends BaseEventResponse {
        public Result result;

        public static class Result {
            public String previousOwner;

            public String newOwner;
        }
    }

    public static class SendAuthMessageEventResponse extends BaseEventResponse {
        public Result result;

        public static class Result {
            public byte[] pkg;
        }
    }

    public static class SubProtocolUpdateEventResponse extends BaseEventResponse {
        public Result result;

        public static class Result {
            public java.lang.Long protocolType;

            public String protocolAddress;
        }
    }

    public static class RecvAuthMessageEventResponse extends BaseEventResponse {
        public Result result;

        public static class Result {
            public String recvDomain;

            public byte[] rawMsg;
        }
    }
}
