
# 使用说明

## 构建

```bash
cd /path/to/offchain-plugin
mvn package
```

## 使用

参考[插件服务](https://github.com/AntChainOpenLab/AntChainBridgePluginServer/blob/main/README.md)（PluginServer, PS）的使用，将jar包放到指定路径，通过PS加载即可


## 配置


建议在`sdk_config.yml`中配置证书文件路径时使用**绝对路径**

一个插件的证书配置文件路径参考如下：
```
├── src
│   ├── main
│   │   ├── java
│   │   └── resources
│   │       ├── crypto-config
│   │       │   └── org_id
│   │       │       └── users
│   │       │           └── user1
│   │       │               ├── user1.sign.crt
│   │       │               ├── user1.sign.key
│   │       │               ├── user1.tls.crt
│   │       │               └── user1.tls.key
│   │       └── sdk_config.yml
│   └── test
└── target

```


## 贡献者

上海浦江数链数字科技有限公司

长三角数链（上海）网络基础设施有限公司
