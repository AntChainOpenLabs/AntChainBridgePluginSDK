package com.alipay.antchain.bridge.relayer.core.manager.bbc;

import com.alipay.antchain.bridge.commons.core.base.SendResponseResult;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgPackage;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlock;

import java.util.List;

/**
 * 抽象的AMClient服务合约
 * <p>
 * 不同的链有不同的实现。
 */
public interface IAMClientContract {

    SendResponseResult recvPkgFromRelayer(AuthMsgPackage pkg);

    /**
     * 添加AM上层协议
     *
     * @param protocolContract 协议合约地址
     * @param protocolType     协议类型
     * @return
     */
    void setProtocol(String protocolContract, String protocolType);

    /**
     * 设置Ptc合约地址
     *
     * @param ptcContractAddress
     */
    void setPtcContract(String ptcContractAddress);

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // 非合约接口，合约的数据解析接口
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * 解析区块里的合约发出的AM消息
     *
     * @param abstractBlock
     * @return
     */
    List<AuthMsgWrapper> parseAMRequest(AbstractBlock abstractBlock);

    /**
     * 部署合约
     */
    void deployContract();
}
