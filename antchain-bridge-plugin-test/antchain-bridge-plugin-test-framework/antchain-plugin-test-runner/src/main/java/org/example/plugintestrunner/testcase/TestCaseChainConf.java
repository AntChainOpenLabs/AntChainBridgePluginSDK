package org.example.plugintestrunner.testcase;

import lombok.Getter;
import lombok.Setter;

// 用于存放 chainConf 配置的类
@Setter
@Getter
public class TestCaseChainConf {
    // Getter 和 Setter 方法
    private long gasLimit;
    private long gasPrice;
    private String privateKey;
    private String url;


    public boolean isValid() {
        return !(gasLimit == 0 || gasPrice == 0 || privateKey == null || url == null);
    }
}
