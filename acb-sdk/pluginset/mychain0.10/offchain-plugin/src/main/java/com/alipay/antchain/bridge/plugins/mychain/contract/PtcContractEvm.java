/*
 * Copyright 2024 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.plugins.mychain.contract;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.bbc.syscontract.PTCContract;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.base.SendResponseResult;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTrustRoot;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import com.alipay.antchain.bridge.commons.core.ptc.PTCVerifyAnchor;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyBlockchainTrustAnchor;
import com.alipay.antchain.bridge.plugins.mychain.exceptions.CallContractException;
import com.alipay.antchain.bridge.plugins.mychain.sdk.Mychain010Client;
import com.alipay.mychain.sdk.api.utils.Utils;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.alipay.mychain.sdk.domain.transaction.TransactionReceipt;
import com.alipay.mychain.sdk.message.transaction.TransactionReceiptResponse;
import com.alipay.mychain.sdk.vm.EVMOutput;
import com.alipay.mychain.sdk.vm.EVMParameter;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;

public class PtcContractEvm extends PTCContract implements AbstractPtcContract {

    private static final String PTC_HUB_EVM_CONTRACT_PREFIX = "PTC_HUB_EVM_CONTRACT_";

    private Mychain010Client mychain010Client;

    private final Logger logger;

    public PtcContractEvm(Mychain010Client mychain010Client, Logger logger) {
        this.mychain010Client = mychain010Client;
        this.logger = logger;
    }

    @Override
    public boolean deployContract(String bcdnsRootCertPem) {
        String contractPath = MychainContractBinaryVersionEnum.selectBinaryByVersion(
                mychain010Client.getConfig().getMychainContractBinaryVersion()
        ).getPtcHubEvm();
        if (StrUtil.isEmpty(contractPath)) {
            logger.error("no ptc hub binary code for this version of contracts");
            return false;
        }

        if (StringUtils.isEmpty(this.getContractAddress())) {

            String contractName = PTC_HUB_EVM_CONTRACT_PREFIX + UUID.randomUUID();

            CommitteePtcVerifierContractEvm committeePtcVerifierContractEvm = new CommitteePtcVerifierContractEvm(mychain010Client, logger);
            if (!committeePtcVerifierContractEvm.deployContract()) {
                logger.error("failed to deploy committee ptc verifier contract");
                return false;
            }

            logger.info("Deploying PTC hub contract {} with code from {}", contractName, contractPath);
            AbstractCrossChainCertificate bcdnsRootCert = CrossChainCertificateUtil.readCrossChainCertificateFromPem(bcdnsRootCertPem.getBytes());
            if (bcdnsRootCert.getType() != CrossChainCertificateTypeEnum.BCDNS_TRUST_ROOT_CERTIFICATE) {
                logger.error("for now, PTC hub contract only support BCDNS trust root certificate to initialize");
                return false;
            }

            EVMParameter parameter = new EVMParameter();
            parameter.addBytes(bcdnsRootCert.encode());
            if (!mychain010Client.deployContract(contractPath, contractName, VMTypeEnum.EVM, parameter)) {
                logger.error("failed to deploy ptc hub contract");
                return false;
            }
            this.setContractAddress(contractName);

            logger.info("Set committee verifier {} into ptc hub now...", committeePtcVerifierContractEvm.getContractAddress());
            setCommitteeVerifier(committeePtcVerifierContractEvm.getContractAddress());

            this.setStatus(ContractStatusEnum.CONTRACT_READY);
            return true;
        }

        return true;
    }

    @Override
    public void updatePTCTrustRoot(PTCTrustRoot ptcTrustRoot) {
        EVMParameter parameters = new EVMParameter("updatePTCTrustRoot(bytes)");
        parameters.addBytes(ptcTrustRoot.encode());

        SendResponseResult result = mychain010Client.callContract(this.getContractAddress(), parameters, true);
        if (!result.isSuccess()) {
            throw new CallContractException(getContractAddress(), result.getTxId(), result.getErrorMessage());
        }

        logger.info("Update PTC trust root {} with tx {} successfully!",
                ptcTrustRoot.getPtcCredentialSubject().getApplicant().toHex(), result.getTxId());
    }

    public void setCommitteeVerifier(String committeeVerifierName) {
        EVMParameter parameters = new EVMParameter("addPtcVerifier(identity)");
        parameters.addIdentity(Utils.getIdentityByName(committeeVerifierName, mychain010Client.getConfig().getMychainHashType()));

        SendResponseResult result = mychain010Client.callContract(this.getContractAddress(), parameters, true);
        if (!result.isSuccess()) {
            throw new CallContractException(getContractAddress(), result.getTxId(), result.getErrorMessage());
        }

        logger.info("Set committee verifier {} with tx {} successfully!", committeeVerifierName, result.getTxId());
    }

    public String getCommitteeVerifier() {
        EVMParameter parameters = new EVMParameter("verifierMap(uint8)");
        parameters.addUint(BigInteger.valueOf(PTCTypeEnum.COMMITTEE.ordinal()));

        TransactionReceipt receipt = mychain010Client.localCallContract(this.getContractAddress(), parameters);
        if (ObjectUtil.isEmpty(receipt.getOutput())) {
            return null;
        }
        return new EVMOutput(Hex.toHexString(receipt.getOutput())).getIdentity().hexStrValue();
    }

    @Override
    public PTCTrustRoot getPTCTrustRoot(ObjectIdentity ptcOwnerOid) {
        EVMParameter parameters = new EVMParameter("getPTCTrustRoot(bytes)");
        parameters.addBytes(ptcOwnerOid.encode());

        TransactionReceipt receipt = mychain010Client.localCallContract(this.getContractAddress(), parameters);
        if (ObjectUtil.isEmpty(receipt.getOutput())) {
            return null;
        }
        byte[] raw = new EVMOutput(Hex.toHexString(receipt.getOutput())).getBytes();
        if (ArrayUtil.isEmpty(raw)) {
            return null;
        }

        return PTCTrustRoot.decode(raw);
    }

    @Override
    public boolean hasPTCTrustRoot(ObjectIdentity ptcOwnerOid) {
        EVMParameter parameters = new EVMParameter("hasPTCTrustRoot(bytes)");
        parameters.addBytes(ptcOwnerOid.encode());

        TransactionReceipt receipt = mychain010Client.localCallContract(this.getContractAddress(), parameters);
        if (ObjectUtil.isEmpty(receipt.getOutput())) {
            return false;
        }
        return new EVMOutput(Hex.toHexString(receipt.getOutput())).getBoolean();
    }

    @Override
    public PTCVerifyAnchor getPTCVerifyAnchor(ObjectIdentity ptcOwnerOid, BigInteger versionNum) {
        EVMParameter parameters = new EVMParameter("getPTCVerifyAnchor(bytes,uint256)");
        parameters.addBytes(ptcOwnerOid.encode());
        parameters.addUint(versionNum);

        TransactionReceipt receipt = mychain010Client.localCallContract(this.getContractAddress(), parameters);
        if (ObjectUtil.isEmpty(receipt.getOutput())) {
            return null;
        }
        byte[] raw = new EVMOutput(Hex.toHexString(receipt.getOutput())).getBytes();
        if (ArrayUtil.isEmpty(raw)) {
            return null;
        }

        return PTCVerifyAnchor.decode(raw);
    }

    @Override
    public boolean hasPTCVerifyAnchor(ObjectIdentity ptcOwnerOid, BigInteger versionNum) {
        EVMParameter parameters = new EVMParameter("hasPTCVerifyAnchor(bytes,uint256)");
        parameters.addBytes(ptcOwnerOid.encode());
        parameters.addUint(versionNum);

        TransactionReceipt receipt = mychain010Client.localCallContract(this.getContractAddress(), parameters);
        if (ObjectUtil.isEmpty(receipt.getOutput())) {
            return false;
        }
        return new EVMOutput(Hex.toHexString(receipt.getOutput())).getBoolean();
    }

    @Override
    public void addTpBta(ThirdPartyBlockchainTrustAnchor tpBta) {
        EVMParameter parameters = new EVMParameter("addTpBta(bytes)");
        parameters.addBytes(tpBta.encode());

        SendResponseResult result = mychain010Client.callContract(this.getContractAddress(), parameters, true);
        if (!result.isSuccess()) {
            throw new CallContractException(getContractAddress(), result.getTxId(), result.getErrorMessage());
        }

        logger.info("Add TpBta {}:{} with tx {} successfully!",
                tpBta.getCrossChainLane().getLaneKey(), tpBta.getTpbtaVersion(), result.getTxId());
    }

    @Override
    public ThirdPartyBlockchainTrustAnchor getTpBta(CrossChainLane tpbtaLane, long tpBtaVersion) {
        EVMParameter parameters = new EVMParameter("getTpBta(bytes,uint32)");
        parameters.addBytes(tpbtaLane.encode());
        parameters.addUint(BigInteger.valueOf(tpBtaVersion));

        TransactionReceipt receipt = mychain010Client.localCallContract(this.getContractAddress(), parameters);
        if (ObjectUtil.isEmpty(receipt.getOutput())) {
            return null;
        }
        byte[] raw = new EVMOutput(Hex.toHexString(receipt.getOutput())).getBytes();
        if (ArrayUtil.isEmpty(raw)) {
            return null;
        }

        return ThirdPartyBlockchainTrustAnchor.decode(raw);
    }

    @Override
    public ThirdPartyBlockchainTrustAnchor getLatestTpBta(CrossChainLane tpbtaLane) {
        EVMParameter parameters = new EVMParameter("getLatestTpBta(bytes)");
        parameters.addBytes(tpbtaLane.encode());

        TransactionReceipt receipt = mychain010Client.localCallContract(this.getContractAddress(), parameters);
        if (ObjectUtil.isEmpty(receipt.getOutput())) {
            return null;
        }
        byte[] raw = new EVMOutput(Hex.toHexString(receipt.getOutput())).getBytes();
        if (ArrayUtil.isEmpty(raw)) {
            return null;
        }

        return ThirdPartyBlockchainTrustAnchor.decode(raw);
    }

    @Override
    public boolean hasTpBta(CrossChainLane tpbtaLane, long tpBtaVersion) {
        EVMParameter parameters = new EVMParameter("hasTpBta(bytes,uint32)");
        parameters.addBytes(tpbtaLane.encode());
        parameters.addUint(BigInteger.valueOf(tpBtaVersion));

        TransactionReceipt receipt = mychain010Client.localCallContract(this.getContractAddress(), parameters);
        if (ObjectUtil.isEmpty(receipt.getOutput())) {
            return false;
        }
        return new EVMOutput(Hex.toHexString(receipt.getOutput())).getBoolean();
    }

    @Override
    public Set<PTCTypeEnum> getSupportedPTCTypes() {
        TransactionReceipt receipt = mychain010Client.localCallContract(this.getContractAddress(), new EVMParameter("getSupportedPTCType()"));
        if (ObjectUtil.isEmpty(receipt.getOutput())) {
            return new HashSet<>();
        }
        Set<PTCTypeEnum> ptcTypeSet = new HashSet<>();
        for (BigInteger val : new EVMOutput(Hex.toHexString(receipt.getOutput())).getUintDynamicArray()) {
            ptcTypeSet.add(PTCTypeEnum.valueOf(val.byteValueExact()));
        }

        return ptcTypeSet;
    }
}
