
# 使用说明

## 构建

```bash
cd /path/to/offchain-plugin
mvn package
```

## 使用
请在插件启动前部署系统合约，获得合约地址，并在`plugin_config.yml`中添加配置，例如：
```yaml
am_contract_address:
  0x5EcA8b82FdDA7F7c5A3fFdE0B3F34A8eD9A3b71
sdp_contract_address:
  0x3FcAdD5C9aF0A3EfB8A2a5A9C0D2D2f0A1b2E34
```
启动插件请参考[插件服务](https://github.com/AntChainOpenLab/AntChainBridgePluginServer/blob/main/README.md)（PluginServer, PS）的使用，将jar包放到指定路径，通过PS加载即可

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
│   │       └── plugin_config.yml
│   └── test
└── target

```

## 支持版本 

chaimaker `v2.3.2`


chainmaker-sdk-java `v2.3.2`


## 贡献者

上海浦江数链数字科技有限公司

长三角数链（上海）网络基础设施有限公司
