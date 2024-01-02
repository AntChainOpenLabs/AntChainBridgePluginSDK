package com.alipay.antchain.bridge.plugins.mychain.contract;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.plugins.mychain.sdk.Mychain010Client;
import com.alipay.mychain.sdk.api.utils.Utils;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.alipay.mychain.sdk.crypto.hash.Hash;
import com.alipay.mychain.sdk.domain.transaction.TransactionReceipt;
import com.alipay.mychain.sdk.vm.EVMOutput;
import com.alipay.mychain.sdk.vm.EVMParameter;
import lombok.Getter;
import lombok.Setter;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Getter
@Setter
public class SDPContractClientEVM extends SDPContract implements AbstractSDPContractClient {

    private static final String SDP_EVM_CONTRACT_PREFIX = "SDP_EVM_CONTRACT_";

    private static final String SET_AM_CONTRACT_AND_DOMAIN_SIGN = "SetAmContractAndDomain(identity,string)";
    private static final String QUERY_SDP_SEQ_SIGN = "queryP2PMsgSeqOnChain(bytes,identity,bytes,identity)";

    private static final Logger LOGGER = LoggerFactory.getLogger(SDPContractClientEVM.class);

    private Mychain010Client mychain010Client;

    public String localDomain;

    public SDPContractClientEVM(Mychain010Client mychain010Client) {
        this.mychain010Client = mychain010Client;
    }

    @Override
    public boolean setAmContractAndDomain(String amContractName) {
        EVMParameter parameters = new EVMParameter(SET_AM_CONTRACT_AND_DOMAIN_SIGN);
        parameters.addIdentity(Utils.getIdentityByName(
                amContractName,
                mychain010Client.getConfig().getMychainHashType()));
        parameters.addString(localDomain);

        if (mychain010Client.callContract(
                        this.getContractAddress(),
                        parameters,
                        true)
                .isSuccess()) {
            this.setStatus(ContractStatusEnum.CONTRACT_READY);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public long queryP2PMsgSeqOnChain(String senderDomain,
                                      String from,
                                      String receiverDomain,
                                      String to) {
        EVMParameter parameters = new EVMParameter(QUERY_SDP_SEQ_SIGN);
        parameters.addBytes(senderDomain.getBytes());
        parameters.addIdentity(new Hash(from));
        parameters.addBytes(receiverDomain.getBytes());
        parameters.addIdentity(new Hash(to));

        // TODO: handle errors
        TransactionReceipt receipt = mychain010Client.localCallContract(
                this.getContractAddress(),
                parameters);
        if (null == receipt.getOutput()) {
            return 0;
        }

        EVMOutput evmOutput = new EVMOutput(Hex.toHexString(receipt.getOutput()));
        int value = evmOutput.getInt().intValue();
        return (long) value;
    }

    @Override
    public boolean deployContract() {
        if (StrUtil.isEmpty(this.getContractAddress())) {

            String contractPath = MychainContractBinaryVersionEnum.selectBinaryByVersion(
                    mychain010Client.getConfig().getMychainContractBinaryVersion()).getSdpEvm();
            String contractName = SDP_EVM_CONTRACT_PREFIX + UUID.randomUUID().toString();

            if (mychain010Client.deployContract(
                    contractPath,
                    contractName,
                    VMTypeEnum.EVM,
                    new EVMParameter())) {
                this.setContractAddress(contractName);
                this.setStatus(ContractStatusEnum.CONTRACT_DEPLOYED);
                return true;
            } else {
                return false;
            }
        }

        return true;
    }

}
