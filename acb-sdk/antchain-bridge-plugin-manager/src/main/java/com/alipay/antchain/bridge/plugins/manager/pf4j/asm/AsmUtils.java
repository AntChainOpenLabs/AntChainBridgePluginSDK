package com.alipay.antchain.bridge.plugins.manager.pf4j.asm;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.plugins.lib.BBCService;
import com.alipay.antchain.bridge.plugins.lib.HeteroChainDataVerifierService;
import lombok.SneakyThrows;

public class AsmUtils {
    @SneakyThrows
    public static IAntChainBridgeServiceInfo load(String className, ClassLoader classLoader) {
        Class<?> clz = classLoader.loadClass(className);
        if (ObjectUtil.isNotNull(clz.getAnnotation(BBCService.class))) {
            return BBCServiceInfo.load(className, classLoader);
        } else if (ObjectUtil.isNotNull(clz.getAnnotation(HeteroChainDataVerifierService.class))) {
            return HCDVSServiceInfo.load(className, classLoader);
        } else {
            throw new IllegalArgumentException(StrUtil.format("classname {} neither BBC or HCDVS", className));
        }
    }
}
