package com.alipay.antchain.bridge.plugins.mychain.sdp;

import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.bridge.commons.core.base.SendResponseResult;
import com.alipay.antchain.bridge.plugins.mychain.common.BizContractTypeEnum;
import com.alipay.antchain.bridge.plugins.mychain.common.SDPMsgTypeEnum;
import com.alipay.antchain.bridge.plugins.mychain.sdk.Mychain010Client;
import com.alipay.mychain.sdk.api.utils.Utils;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.alipay.mychain.sdk.domain.account.Identity;
import com.alipay.mychain.sdk.vm.EVMOutput;
import com.alipay.mychain.sdk.vm.EVMParameter;
import lombok.SneakyThrows;

import java.util.Objects;
import java.util.UUID;

public class SenderEvm extends AbstractDemoSenderContract {

    private static final String BINARY_BYTE_CODE_RESOURCE_PATH = "/contracts/demo_v2/sender.bin";

    private static final String RUNTIME_BYTE_CODE_RESOURCE_PATH = "/contracts/demo_v2/sender.bin-runtime";

    public SenderEvm(String domainName, Mychain010Client mychain010Client, String sdpContractName) {
        this.setDomain(domainName);
        this.setMychain010Client(mychain010Client);
        this.setContractName("SenderEvm_" + UUID.randomUUID());
        this.setSdpContractName(sdpContractName);
        if (!deployOrUpgradeContract() || !setSDPMSGAddress()) {
            throw new RuntimeException("Failed to setup SenderEvm.");
        }
    }

    @Override
    public BizContractTypeEnum getBizContractType() {
        return BizContractTypeEnum.SOLIDITY;
    }

    @Override
    public SendResponseResult sendMsgV1To(String receiverDomain, Identity receiverContractID, String msg, SDPMsgTypeEnum sdpType) {
        EVMParameter parameters = new EVMParameter(sdpType.getEvmMethodToSendV1Msg());
        parameters.addIdentity(receiverContractID);
        parameters.addString(receiverDomain);
        parameters.addBytes(msg.getBytes());
        return this.getMychain010Client().callContract(this.getContractName(), parameters, true);
    }

    @Override
    public boolean sendMsgV2To(String receiverDomain, Identity receiverContractID, String msg, boolean isAtomic, SDPMsgTypeEnum sdpType) {
        EVMParameter parameters = new EVMParameter(sdpType.getEvmMethodToSendV2Msg());
        parameters.addIdentity(receiverContractID);
        parameters.addString(receiverDomain);
        parameters.addBool(isAtomic);
        parameters.addBytes(msg.getBytes());
        return this.getMychain010Client().callContract(this.getContractName(), parameters, true).isSuccess();
    }

    @Override
    public String getLatestMsgIdSentUnorder() {
        byte[] output = Objects.requireNonNull(
                this.getMychain010Client().localCallContract(
                        this.getContractName(),
                        new EVMParameter(SDPMsgTypeEnum.UNORDERED.getEvmMethodToGetLastMsgIdSent())
                )
        ).getOutput();
        return HexUtil.encodeHexStr(new EVMOutput(HexUtil.encodeHexStr(output)).getBytes32());
    }

    @Override
    public String getLatestMsgIdSentOrder() {
        byte[] output = Objects.requireNonNull(
                this.getMychain010Client().localCallContract(
                        this.getContractName(),
                        new EVMParameter(SDPMsgTypeEnum.ORDERED.getEvmMethodToGetLastMsgIdSent())
                )
        ).getOutput();
        return HexUtil.encodeHexStr(new EVMOutput(HexUtil.encodeHexStr(output)).getBytes32());
    }

    @Override
    public String getLatestMsgIdAckSuccess() {
        byte[] output = Objects.requireNonNull(
                this.getMychain010Client().localCallContract(
                        this.getContractName(),
                        new EVMParameter("latest_msg_id_ack_success()")
                )
        ).getOutput();
        return HexUtil.encodeHexStr(new EVMOutput(HexUtil.encodeHexStr(output)).getBytes32());
    }

    @Override
    public String getLatestMsgIdAckError() {
        byte[] output = Objects.requireNonNull(
                this.getMychain010Client().localCallContract(
                        this.getContractName(),
                        new EVMParameter("latest_msg_id_ack_error()")
                )
        ).getOutput();
        return HexUtil.encodeHexStr(new EVMOutput(HexUtil.encodeHexStr(output)).getBytes32());
    }

    @Override
    public String getLatestMsgError() {
        byte[] output = Objects.requireNonNull(
                this.getMychain010Client().localCallContract(
                        this.getContractName(),
                        new EVMParameter("latest_msg_error()")
                )
        ).getOutput();
        return new EVMOutput(HexUtil.encodeHexStr(output)).getString();
    }

    private boolean setSDPMSGAddress() {
        EVMParameter parameters = new EVMParameter("setSDPMSGAddress(identity)");
        parameters.addIdentity(Utils.getIdentityByName(getSdpContractName()));
        return this.getMychain010Client().callContract(this.getContractName(), parameters, true).isSuccess();
    }

    @SneakyThrows
    private boolean deployOrUpgradeContract() {
        if (this.getMychain010Client().deployContract(
                BINARY_BYTE_CODE_RESOURCE_PATH,
                getContractName(),
                VMTypeEnum.EVM,
                new EVMParameter()
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
