package com.alipay.antchain.bridge.plugins.ethereum.helper.model;

import com.alipay.antchain.bridge.plugins.ethereum.helper.GasPriceProviderSupplierEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GasPriceProviderConfig {
    private GasPriceProviderSupplierEnum gasPriceProviderSupplier;

    private String gasProviderUrl;

    private String apiKey;

    private long gasUpdateInterval;
}
