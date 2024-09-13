package com.ali.antchain.abstarct;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;

// 需要不同测试工具实现的定制化测试操作
public interface ITester {

    /**
     * 根据am合约地址调用链上am合约的getProtocol方法，返回protocol合约地址，用于验证setProtocol方法是否正确执行
     *
     * @param amContractAddr
     * @return
     */
    public String getProtocol(String amContractAddr);

    /**
     * 向链上部署app合约，并设置app合约中的protocol合约地址，返回app合约地址
     * @param protocolAddr
     * @return
     */
    public byte[] deployApp(String protocolAddr);

    /**
     * 查询链上txHash的交易是否已上链，会重试一定次数直至已上链
     * @param txhash
     */
    public void waitForTxConfirmed(String txhash);

}
