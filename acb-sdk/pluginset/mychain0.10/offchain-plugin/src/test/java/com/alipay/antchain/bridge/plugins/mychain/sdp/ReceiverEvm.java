package com.alipay.antchain.bridge.plugins.mychain.sdp;

import java.math.BigInteger;
import java.util.UUID;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.plugins.mychain.common.BizContractTypeEnum;
import com.alipay.antchain.bridge.plugins.mychain.common.SDPMsgTypeEnum;
import com.alipay.antchain.bridge.plugins.mychain.sdk.Mychain010Client;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.alipay.mychain.sdk.domain.account.Identity;
import com.alipay.mychain.sdk.domain.transaction.TransactionReceipt;
import com.alipay.mychain.sdk.vm.EVMOutput;
import com.alipay.mychain.sdk.vm.EVMParameter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;

@Slf4j
public class ReceiverEvm extends AbstractDemoReceiverContract {

    private static final String BINARY_BYTE_CODE_RESOURCE_PATH = "/contracts/demo_v2/receiver.bin";

    private static final String RUNTIME_BYTE_CODE_RESOURCE_PATH = "/contracts/demo_v2/receiver.bin-runtime";

    public ReceiverEvm(String domainName, Mychain010Client mychain010Client, String sdpContractName) {
        this.setDomain(domainName);
        this.setMychain010Client(mychain010Client);
        this.setContractName("ReceiverEvm_" + UUID.randomUUID());
        this.setSdpContractName(sdpContractName);
        if (!deployOrUpgradeContract()) {
            throw new RuntimeException("Failed to setup ReceiverEvm.");
        }
    }

    @Override
    public BizContractTypeEnum getBizContractType() {
        return BizContractTypeEnum.SOLIDITY;
    }

    @Override
    public boolean setRecvSequence(String senderDomain, Identity senderContractID, int recvSeq) {
        String method = "setMsgSeq(string,identity,identity,uint32)";
        EVMParameter parameters = new EVMParameter(method);
        parameters.addString(senderDomain);
        parameters.addIdentity(senderContractID);
        parameters.addIdentity(getContractId());
        parameters.addUint(BigInteger.valueOf(recvSeq));
        return getMychain010Client().callContract(getSdpContractName(), parameters, true).isSuccess();
    }

    @Override
    public String getLastMsg(SDPMsgTypeEnum sdpType) {
        EVMParameter parameters = new EVMParameter(sdpType.getEvmMethodToGetLastMsg());
        TransactionReceipt receipt = this.getMychain010Client().localCallContract(this.getContractName(), parameters);
        if (ObjectUtil.isNull(receipt.getOutput())) {
            return null;
        }

        EVMOutput EVMOutput = new EVMOutput(Hex.toHexString(receipt.getOutput()));
        String lastMsg = new String(EVMOutput.getBytes());
        log.info("last {} msg is {}", sdpType, lastMsg);

        return lastMsg;
    }

    @SneakyThrows
    private boolean deployOrUpgradeContract() {
        if (this.getMychain010Client().deployContract(
                BINARY_BYTE_CODE_RESOURCE_PATH, this.getContractName(), VMTypeEnum.EVM, new EVMParameter()
        )) {
            return true;
        }
        return this.getMychain010Client().upgradeContract(
                RUNTIME_BYTE_CODE_RESOURCE_PATH,
                this.getContractName(),
                VMTypeEnum.EVM
        );
    }
}
