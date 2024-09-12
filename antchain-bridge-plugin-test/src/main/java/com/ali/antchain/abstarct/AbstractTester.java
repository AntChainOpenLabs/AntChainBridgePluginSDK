package com.ali.antchain.abstarct;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

/**
 * 插件测试工具需要实现的定制化操作
 * todo: 这里的方法肯定不能抛出异常啊！有异常的在接口里面处理妥当！
 */
@Getter
@Setter
public abstract class AbstractTester implements ITester {

    private Logger bbcLogger;
    public AbstractTester() {
        bbcLogger = NOPLogger.NOP_LOGGER;
    }
}
