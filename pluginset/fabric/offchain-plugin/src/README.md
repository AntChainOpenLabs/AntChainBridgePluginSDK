<div align="center">
  <img alt="am logo" src="https://gw.alipayobjects.com/zos/bmw-prod/3ee4adc7-1960-4dbf-982e-522ac135a0c0.svg" width="250" >
  <h1 align="center">ODATS CLI</h1>
  <p align="center">
    <a href="http://makeapullrequest.com">
      <img alt="pull requests welcome badge" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat">
    </a>
  </p>
</div>

## 介绍

CLI工具是用于管理中继的交互式命令行工具，它可以完成区块链授权、注册、停止服务等工作。
​

## 使用

### 在私有化部署中使用

#### 启动

如果您使用私有化部署，在使用docker_start.sh启动工作容器relayer_1之后，您可以通过以下命令找到CLI工具：

```
docker exec -it relayer_1 bash
cd /home/admin/release/os/os-cli
java -jar odats-cli-release.jar -p 18088
```

18088为配置文件中的`admin.server.port`字段。启动后，会有如下输出：

```
------------------------------------------------------------
------------------------------------------------------------
----                                                   -----
----               ODATS CONSOLE                     -----
----                                                   -----
------------------------------------------------------------
------------------------------------------------------------
type help to see more...
odats>
```

当然也可以 [下载](https://mytf-dev2.oss-cn-shanghai.aliyuncs.com/odats_deploy/tools/target_cli.tar.gz?OSSAccessKeyId=LTAI4GGDgA7iB6gwwWgZ9GKS&Expires=5239484330&Signature=I4cBVGErNP%2F6rqbYoXElBFPLO7o%3D) 使用。
解压压缩包之后，运行`bin`目录下的`start.sh`即可。

#### CLI完成授权

- 完成数据授权：

被授权方域名的链可以获取授权方域名链的数据，类型为0，代表MYCHAIN_TX。
类型表如下：

| 类型                 | 数字 |
| -------------------- | ---- |
| DUMMY                | -1   |
| MYCHAIN_TX           | 0    |
| MYCHAIN_BLOCK_HEADER | 1    |
| MYCHAIN_BLOCK        | 2    |
| FABRIC_TX            | 3    |

```
odats>serviceManager.addAccessChainACL("授权方域名", "被授权方域名", 0)
```

- 完成跨链消息授权：

完成授权后，被授权方域名上的合约可作为发送者，发送跨链消息到授权方域名的接收者合约。如果是Fabric链，则合约名字即为链码名字。

```
odats>serviceManager.addCrossChainMsgACL("授权方域名", "接收者的合约名字", "被授权方域名", "发送者的合约名字")
```


#### CLI注册区块链

##### 1. 申请域名证书

首先注册域名证书，如果中继设置了域名空间，则要求域名格式必须符合域名空间要求，即域名包含域名空间为后缀。
mychain_0.10是申请域名证书的类型，比如蚂蚁链**mychain_0.10**、Fabric是**fabric_1.4。YOUR_ID**是该区块链的ID，这里可以自己定义，建议使用【YOUR_DOMAIN + ".id"】。**YOUR_DOMAIN**是你要注册的域名，注册后，该域名与该区块链为绑定的关系。

```
odats>serviceManager.applyDomainCert("mychain_0.10", "YOUR_ID", "YOUR_DOMAIN")
```

运行成功的话，会有“apply success, cert : ”加上一个Base64的字符串，这部分字符串就是要用的域名证书，中继会将其记录在DB中。

##### 2. 签署UDNS

完成域名证书之后，要让中继为区块链的元数据进行签名，生成一个UDNS，来绑定区块链和中继。

```
odats>serviceManager.signUDNS("YOUR_DOMAIN", "mychain_0.10", '{"all_nodes_pk":"d1246274caa28e3506796494474d73f346594772017bba385e30b205ba98dbcf784ad8679f79e893814a5d82b5fba17b92e0437a8eced2418daf3b84cc3ba23b^18d900fa217a0b090f458df6c1235621f7431086454a4bc44a5c63422a4dc305afabb3a459f6ad0207ee8be6212f55f6c48722094391a626f57cfc7d2561900d^b65c0546a7876acfc7dee47e92b5d69bca18951e4631651ae1271041852d912727732a740cee15e4ec13142284d9752a0d46e4acdda442be362b5fe7f54fbb1f^9ddd87e8c8db6889b9163cb4a23ba3911f107fcaea2db8af1e903b21849bf6b22f6719eca99c654d958dac0e32540c85b6e133f1e63c77c0704ab45ee7674c06","block_hash":"2a43f6f7dd327380b7346af6b20d3eade1c1a06f5c07cacb86150988043de5f4","block_height":"21787313"}')
```

YOUR_DOMAIN是该链的域名，和第一步相同。mychain_0.10是该链的类型，和第一步相同。第三部分是该链的元数据，一个json格式字符串，**这部分数据需要问区块链的管理员索要。**
这里要提前去准备这部分数据，以下样例按照pretty format来展示：
例1：Mychain的元数据。**​**

| 字段         | 解释                                          |
| ------------ | --------------------------------------------- |
| all_nodes_pk | Mychain网络中所有共识节点的公钥，用于验证签名 |
| block_hash   | 区块链从该区块开始接入跨链系统。              |
| block_height | 区块链从该区块高度开始接入跨链系统。          |

```json
{
	"all_nodes_pk":"d1246274caa28e3506796494474d73f346594772017bba385e30b205ba98dbcf784ad8679f79e893814a5d82b5fba17b92e0437a8eced2418daf3b84cc3ba23b^18d900fa217a0b090f458df6c1235621f7431086454a4bc44a5c63422a4dc305afabb3a459f6ad0207ee8be6212f55f6c48722094391a626f57cfc7d2561900d^b65c0546a7876acfc7dee47e92b5d69bca18951e4631651ae1271041852d912727732a740cee15e4ec13142284d9752a0d46e4acdda442be362b5fe7f54fbb1f^9ddd87e8c8db6889b9163cb4a23ba3911f107fcaea2db8af1e903b21849bf6b22f6719eca99c654d958dac0e32540c85b6e133f1e63c77c0704ab45ee7674c06",
	"block_hash":"2a43f6f7dd327380b7346af6b20d3eade1c1a06f5c07cacb86150988043de5f4",
	"block_height":"21787313"
}
```

例2：Fabric的元数据。

| 字段                | 解释                                                   |
| ------------------- | ------------------------------------------------------ |
| channel_id          | 这里是要跨链的channel名字。                            |
| chaincode_name      | 跨链链码名字，每个channel是唯一的。                    |
| threshold           | 至少N个validator对某消息做出背书，中继才会信任该消息。 |
| all_validators_cert | validator集合（peer节点）的身份证书，用“^”分隔。       |
| policy_type         | 默认为0，这里使用0即可。                               |

```json
{
    "channel_id":"mychannel",
    "chaincode_name":"odatscrosschaincc5",
    "threshold":"2",
    "all_validators_cert":"^-----BEGIN CERTIFICATE-----\nMIICKTCCAc+gAwIBAgIRAOdXYQTbaGTp2CfySyHxkSYwCgYIKoZIzj0EAwIwczEL\nMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBG\ncmFuY2lzY28xGTAXBgNVBAoTEG9yZzIuZXhhbXBsZS5jb20xHDAaBgNVBAMTE2Nh\nLm9yZzIuZXhhbXBsZS5jb20wHhcNMjEwOTIyMDkyNDU5WhcNMzEwOTIwMDkyNDU5\nWjBqMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMN\nU2FuIEZyYW5jaXNjbzENMAsGA1UECxMEcGVlcjEfMB0GA1UEAxMWcGVlcjAub3Jn\nMi5leGFtcGxlLmNvbTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABHF4bEYkCZDv\nZQHW93vXi/PWfvdUISypyCUoy5Rwz82GPs5aVaBKcXTBEA7rRdKT3O+6/kK6/B7g\ntZADvsJGDqmjTTBLMA4GA1UdDwEB/wQEAwIHgDAMBgNVHRMBAf8EAjAAMCsGA1Ud\nIwQkMCKAIEZTQmuwfbLNg1BIrHVIBtydnKmK7t4nrORQFLTSlp2dMAoGCCqGSM49\nBAMCA0gAMEUCIQDQnhyv1w7DaINJRaAgNfKu5f7GZspDcsqs9SgsyQGnJQIgAJzs\nnF2tAMA1WNSduIevyau0RR5kiKis2c4w+f87fnw=\n-----END CERTIFICATE-----\n^-----BEGIN CERTIFICATE-----\nMIICKTCCAc+gAwIBAgIRAMNJF0x6ycd9uv+7MpTiTiIwCgYIKoZIzj0EAwIwczEL\nMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBG\ncmFuY2lzY28xGTAXBgNVBAoTEG9yZzEuZXhhbXBsZS5jb20xHDAaBgNVBAMTE2Nh\nLm9yZzEuZXhhbXBsZS5jb20wHhcNMjEwOTIyMDkyNDU5WhcNMzEwOTIwMDkyNDU5\nWjBqMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMN\nU2FuIEZyYW5jaXNjbzENMAsGA1UECxMEcGVlcjEfMB0GA1UEAxMWcGVlcjAub3Jn\nMS5leGFtcGxlLmNvbTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABNmJoivhQ7iA\nWVTYnwgQAp0K+LSFDHLorOBlxcghYGOxJtqXdzkG/v9S7jCMiCPwPM5aKZC9R7pF\nqfH4JwIutFujTTBLMA4GA1UdDwEB/wQEAwIHgDAMBgNVHRMBAf8EAjAAMCsGA1Ud\nIwQkMCKAIM92yvhPXCN9KKEQA0CkTQD/spKx9JoQ43l7GHaJMTrWMAoGCCqGSM49\nBAMCA0gAMEUCIQCV18hs/WpgF2Y9OgnSVdBLTOEPHSJ2DK/LQsnYMZl6twIgKcyy\nBUnv5jEGGaPsn2idA63oEMs17z7thlmztmioIKI=\n-----END CERTIFICATE-----\n",
    "policy_type":"0"
}
```

运行命令成功后，中继会将UDNS存储到DB，同时命令行会返回成功信息，信息中有一个Base64的字符串，将它单独记录下来，将用于下一步。

##### 3. 注册UDNS

注册UDNS会触发中继的验证逻辑，正式绑定该链到中继。

```
odats>serviceManager.registerUDNS("YOUR_DOMAIN", "上一步记录的Base64字符串")
```

运行完成后，可以看到“registerUDNS success.”。

##### 4. 添加区块链

完成绑定后，需要正式将区块链添加到中继，中继将对其展开监听等操作。

```
odats>blockchainManager.addBlockchianAnchor("mychain_0.10", "YOUR_ID", "", "", '{"mychain_anchor_account":"ccentest","mychain_anchor_account_pub_key":"8e22816b5e1bb6e2e609...e05b53fb785fff","mychain_anchor_account_pri_key":"MIGEAg...Iw4Mm9FXAtDgW1P7eF//","mychain_sslKey":"-----BEGIN ENCRYPTED PRIVATE KEY-----\nMIIE9jAoBgoqhkiG9w0BDAEDMBoEFJRRZ/y...vC6\nJ6NDCMBrbYrVWazDJuFxLUmRSCeT6IQFtKc=\n-----END ENCRYPTED PRIVATE KEY-----","mychain_sslKeyPass":"sslKeyPass","mychain_sslCert":"-----BEGIN CERTIFICATE-----\nMIIEJTCCAw2gAwIBAgIUGve9I2F8...6ogPAqa7z\noXgnTbzTRFHi\n-----END CERTIFICATE-----\n","mychain_trustStore":"/u3+7QAAAAIAAAA...pm8uhKVrjlvTUTM5AlrK18NaryT/IW5n0zX7pkZcKA=","mychain_trustStorePassword":"mychain","mychain_primary":"139.224.168.185 18130","source":"BAAS-CLOUD"}')
```

第一个参数为该区块链的类型；
第二个参数为区块链ID，与第一步相同；
第三个和第四个参数可空置；
第五个参数是区块链网络的描述信息，包含节点地址、中继在该链的账户等信息，**这部分数据需要和客户协调获取**；
执行成功后，会显示“addBlockchianAnchor success.”。
依旧给出Mychain和Fabric的例子，不要尝试使用这里的数据，因为这些数据都有一定的残缺：
例1：Mychain

```json
{
    "mychain_anchor_account":"ccentest",
    "mychain_anchor_account_pub_key":"8e22816b5e1bb6e2e609143b471e116bec7c0d8e57f18170ec0e0e4d737a8e05b53fb785fff",
    "mychain_anchor_account_pri_key":"MIGEAgEAMBAGByqGSM49AgEGBSuBBAAKBG0wawIBAQQgPus4vY1dXako3essxe8DbpFDa3Abr7HwNjlfxgXDsDg5Nc3qFZDYMhvuom67ZYIaBrcpWUJVzIw4Mm9FXAtDgW1P7eF//",
    "mychain_sslKey":"-----BEGIN ENCRYPTED PRIVATE KEY-----\nMIIE9jAoBgoqhkiG9w0BDAEDMBoEFJRRZ/yfZOTN5EXd2NKLMiSmbP3HAgIIAASC\nBMhIYQpEPoZjZ1joMRpemFzdSa8FpPRJBEqMAgEZXE0u3o1RyiHoLPnrO72yNDfZ\nQPJpFqJHkiJELDk5Tjgj1KD0FcSVoz/uMip2WTNf530HyJUbhfHkjUWNFpxYaEan\na8xzeXi1v7a8f8L/0BuPsXHynp7q4isOMfdEoa0oVTJLz/JBpAlUr726qT3YZCec\nl+soSYQmEoCEZFiRqhWa+B0eHc9SzuGLc9frfKB9gzB42XAqT2jvcmhuGRWFamhr\nw2A9SWD6kVaomqb+l+E8SV6FfQ1600nMYAOlxJu/UuX5MM4yHXqrVMuPa4Vmy9Ct\nXEB8xFcdZBpYMETXBtBYNeZIpAu1H2L/7D7XvuzHN6NUzX2oaYC+6ysMr7d6eL1P\nX8lIoUbSDKLxIRNYTlj2gOa5avobazw0oJHfdCcqLgi+BA723BNHsnd7uLJ1fbXv\n9CaZAskvTvmgucUrjCwgAbONda83fF6Hu961M/yYOGF5D0llZBXrTnzRoFZ9v3/K\n1sm615pdVXiSRJlAexmh4pg6t7S8uyTJxdqG8gQ7y3l5mIE3IosilVEYVKlFsvMb\nDN6nj05B/B3JkXOz3UVjb4vk78od+zyDhJ59+QzO59xhLHRhobN3xc2ZxRgQVTdW\n3oWWpHYY/gs2nb2lN0rrvzOl738dSS5aJtlX80gKZefFHqxWoPs+f0osfg95uiy3\nNG9JG+AwmaqEhtKvKgSVfdvv1naaYdrKVczx6BcCH9R8ku4oWPyFGjxPF8fDDB7d\nTAPol3Fb9IHBpwNm3qjbFpG7JFEqvaLOR4EjxN5h8x1QxEnpaUC2FwMRZ8rAC9FP\nAF8MjgTG6ggFGwWxOF3ADvbUEEUbIXFjTWE1GFTCuFDSoZ7OjysYx3bnVBEVuRdm\nieoecor6Ap6hAVli4W0HEKt1MWGiqTAp+6+csR0HaeuSuX5uQoDhqVXxABjOAYgQ\nWEi1OY7H/4R+7c/yuMZKYyEHvC2OAyJgiX/ZWK0m/jWtX9tZFMkUJQiCyIhxuuQt\n1ldxC+k6hr0Z1Vd9CkxmRkC9GF8rt9JlbUEwV8fOdqJb0afuEsq5SbmahgWqUrn1\nItVe+2jS2d57/cU3XjuUhJHvBI5XVd14FO4jgR/DLuuEvZe9r0u4nm1SzhZ3x9fl\n8YKCybVTF5uX6O0dxWYcBLkV62hUoKJHonRymnUGm3gPnKbawcpWn55/4rHsB18E\nD7dnxLqHbZslwb5EcFYyZzTNq4xYyUdweVZq6GhZX2hz8djM5HVdOEZvBpO8MvC6\nJ6NDCMBrbYrVWazDJuFxLUmRSCeT6IQFtKc=\n-----END ENCRYPTED PRIVATE KEY-----",
    "mychain_sslKeyPass":"test.Test0",
    "mychain_sslCert":"-----BEGIN CERTIFICATE-----\nMIIEJTCCAw2gAwIBAgIUGve9I2F82WM3ysaC+fm62w4rYbcwDQYJKoZIhvcNAQEF\nBQAwgYExCzAJBgNVBAYTAkNOMTkwNwYDVQQKDDDljJfkuqzlpKnlqIHor5rkv6Hn\nlLXlrZDllYbliqHmnI3liqHmnInpmZDlhazlj7gxHjAcBgNVBAsMFeWPr+S/oei6\nq+S7vee9kee7nFJTQTEXMBUGA1UEAwwO6JqC6JqB6YeR5pyNQ0EwHhcNMTkxMjEx\nMDE1NzQwWhcNMjIxMjEwMDE1NzQwWjCBgzEPMA0GA1UECgwGQWxpcGF5MRAwDgYD\nVQQLDAdBbGliYWJhMS0wKwYDVQQDDCQwNzcxOGJiMjliMWQ0OGY5Yjg4YTQyY2E1\nZDAxMjZmZV9BbnQxFjAUBgNVBAQMDTE1NzYwMjk0NjA1MzYxFzAVBgNVBAkMDmhh\nbmd6aG91LmNoaW5hMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkBJf\njv/RI6gxOr5npBMygKsPg6TZ6igKeFhYPcxs3sre6OkKhqdGYWrrcaNdZYDMhjbl\nIcXv+Sl1k1/BYsObaub0XJdtTtWIxUjJ8UjeiYaqozQ389pjhcdsqkF6/+5IJlZJ\n59FW2kBcExZd+KJ+XIuo7v6ttUy3Cr0pFQ24zIMvec/AfQv+luTS9YGSqD0wqG/L\nraXJRtC/6Vg6Zo36VGEzGIZEh/Yi37J3GBGAOoqDmg+LNrmaNyAFzLwL9nEV6Udz\n-----END CERTIFICATE-----\n",
    "mychain_trustStore":"/u3+7QAAAAIAAAABAAAAAgACY2EAAAF9uDOuHAAFWC41MDkAAASYMIIElDCCA3ygAwIBAgIUHcqLFDmDlolBBoNv4yhrsVFepTIwDQYJKoZIhvcNAQEFBQAwcDELMAkGA1UEBhMCQ04xEzARBgNVBAoMCmlUcnVzQ2hpbmExHDAaBgNVBAsME0NoaW5hIFRydXN0IE5ldHdvcmsxLjAsBgNVBAMMJWlUcnVzQ2hpbmEgQ2xhc3MgMiBFbnRlcnByaXNlIENBIC0gRzMwHhcNMTcwNzA3MDgxMzU4WhcNMzIwNzA3MDgxMzU4WjCBgTELMAkGA1UEBhMCQ04xOTAhlra/s60TkfAKgQeNWSWUO7fYsEqS5fB0vvljFO5k3L6KGPnY1Q8gvQlcyNfYxDh+B8mqzGfovAhm/2hcQ1JlBZMXWkB2IcKH50R94E24q8r5NjqrPvR+FKUfpJM6Kh0P0HfCExgI+Ixg8SGRh6oJ+dlLwpm8uhKVrjlvTUTM5AlrK18NaryT/IW5n0zX7pkZcKA=",
    "mychain_trustStorePassword":"mychain",
    "mychain_primary":"xxx.224.168.xxx 18130",
    "source":"BAAS-CLOUD"
}
```

解释一下各个字段：

| 字段                           | 解释                             |
| ------------------------------ | -------------------------------- |
| mychain_anchor_account         | 区块链为中继创建的账户，账户名字 |
| mychain_anchor_account_pub_key | 账户公钥                         |
| mychain_anchor_account_pri_key | 账户私钥                         |
| mychain_sslKey                 | 账户的SSL私钥                    |
| mychain_sslKeyPass             | 账户的SSL私钥的密码              |
| mychain_sslCert                | 账户的SSL证书                    |
| mychain_trustStore             | mychain网络的truststore          |
| mychain_trustStorePassword     | mychain truststore的密码         |
| source                         | 来源，可自定义                   |

例2：Fabric
​

```json
{
	"fabric_channel_config":"{\n  \"discoveryPeers\":[\n    {\n      \"peerProperties\":{\n        \"ssl-target-name-override\":\"peer0.org2.example.com\",\n        \"pemBytes\":\"-----BEGIN CERTIFICATE-----\\nMIICSjCCAfCgAwIBAgIRAMCRO4oz1qtN33VV1jiT71MwCgYIKoZIzj0EAwIwdjEL\\nMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBG\\ncmFuY2lzY28xGTAXBgNVBAoTEG9yZzIuZXhhbXBsZS5jb20xHzAdBgNVBAMTFnRs\\nc2NhLm9yZzIuZXhhbXBsZS5jb20wHhcNMjEwOTIyMDkyNDU5WhcNMzEwOTIwMDky\\nNDU5WjB2MQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UE\\nBxMNU2FuIEZyYW5jaXNjbzEZMBcGA1UEChMQb3JnMi5leGFtcGxlLmNvbTEfMB0G\\nA1UEAxMWdGxzY2Eub3JnMi5leGFtcGxlLmNvbTBZMBMGByqGSM49AgEGCCqGSM49\\nAwEHA0IABCQzyayGH4H8/bnB31fL/UdAHmgJSFahznbWUJhHNY40njmIYsSsFsW/\\nLFyA173AxmOZM0BFnmtCtAvosClglu2jXzBdMA4GA1UdDwEB/wQEAwIBpjAPBgNV\\nHSUECDAGBgRVHSUAMA8GA1UdEwEB/wQFMAMBAf8wKQYDVR0OBCIEIAys8XJ2ChOC\\neZGuA4f+ovKhaoS14s/jlJfFam4jftUrMAoGCCqGSM49BAMCA0gAMEUCIQCE2Ul4\\nU73jHOFeI/Xk2CKrR2gLQM04VV2ime6XM0aJ0gIgeljJRz9eQHlzXPjjxxep70bD\\njbLCo7LNPiRGm/UBgzY=\\n-----END CERTIFICATE-----\\n\",\n        \"node_version\":\"1.1.0\"\n      },\n      \"name\":\"peer0.org2.example.com:9051\",\n      \"peerLocation\":\"grpcs://peer0.org2.example.com:9051\"\n    },\n    {\n      \"peerProperties\":{\n        \"ssl-target-name-override\":\"peer0.org1.example.com\",\n        \"pemBytes\":\"-----BEGIN CERTIFICATE-----\\nMIICSTCCAe+gAwIBAgIQHZi+WcoBbQxEPhphIq0RbzAKBggqhkjOPQQDAjB2MQsw\\nCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNU2FuIEZy\\nYW5jaXNjbzEZMBcGA1UEChMQb3JnMS5leGFtcGxlLmNvbTEfMB0GA1UEAxMWdGxz\\nY2Eub3JnMS5leGFtcGxlLmNvbTAeFw0yMTA5MjIwOTI0NTlaFw0zMTA5MjAwOTI0\\nNTlaMHYxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQH\\nEw1TYW4gRnJhbmNpc2NvMRkwFwYDVQQKExBvcmcxLmV4YW1wbGUuY29tMR8wHQYD\\nVQQDExZ0bHNjYS5vcmcxLmV4YW1wbGUuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0D\\nAQcDQgAEmGkYtSwYOowx6gz4Fl+G03MZy/PS69nOySJBEFgqGUjcYVn2dn6TQY2j\\nWuD0PZGN9edcX1OOx2sgeESxADYQWKNfMF0wDgYDVR0PAQH/BAQDAgGmMA8GA1Ud\\nJQQIMAYGBFUdJQAwDwYDVR0TAQH/BAUwAwEB/zApBgNVHQ4EIgQgEq0ojsQ+567S\\nwOMhK5GoKCxbcXNTmZAY9RiPif/k5GIwCgYIKoZIzj0EAwIDSAAwRQIhAI3JaWgj\\npPXBve7MfgH0Afei57g8bzkGCEOl2z97xwTKAiBVRSFfEZjDBd8dwRS43wz3qsl4\\nXr5ZSfj1zPVQEgshxw==\\n-----END CERTIFICATE-----\\n\",\n        \"node_version\":\"1.1.0\"\n      },\n      \"name\":\"peer0.org1.example.com:7051\",\n      \"peerLocation\":\"grpcs://peer0.org1.example.com:7051\"\n    }\n  ],\n  \"validatorThreshold\":2,\n  \"validatorPeers\":[\n    {\n      \"peerProperties\":{\n        \"ssl-target-name-override\":\"peer0.org2.example.com\",\n        \"pemBytes\":\"-----BEGIN CERTIFICATE-----\\nMIICSjCCAfCgAwIBAgIRAMCRO4oz1qtN33VV1jiT71MwCgYIKoZIzj0EAwIwdjEL\\nMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBG\\ncmFuY2lzY28xGTAXBgNVBAoTEG9yZzIuZXhhbXBsZS5jb20xHzAdBgNVBAMTFnRs\\nc2NhLm9yZzIuZXhhbXBsZS5jb20wHhcNMjEwOTIyMDkyNDU5WhcNMzEwOTIwMDky\\nNDU5WjB2MQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UE\\nBxMNU2FuIEZyYW5jaXNjbzEZMBcGA1UEChMQb3JnMi5leGFtcGxlLmNvbTEfMB0G\\nA1UEAxMWdGxzY2Eub3JnMi5leGFtcGxlLmNvbTBZMBMGByqGSM49AgEGCCqGSM49\\nAwEHA0IABCQzyayGH4H8/bnB31fL/UdAHmgJSFahznbWUJhHNY40njmIYsSsFsW/\\nLFyA173AxmOZM0BFnmtCtAvosClglu2jXzBdMA4GA1UdDwEB/wQEAwIBpjAPBgNV\\nHSUECDAGBgRVHSUAMA8GA1UdEwEB/wQFMAMBAf8wKQYDVR0OBCIEIAys8XJ2ChOC\\neZGuA4f+ovKhaoS14s/jlJfFam4jftUrMAoGCCqGSM49BAMCA0gAMEUCIQCE2Ul4\\nU73jHOFeI/Xk2CKrR2gLQM04VV2ime6XM0aJ0gIgeljJRz9eQHlzXPjjxxep70bD\\njbLCo7LNPiRGm/UBgzY=\\n-----END CERTIFICATE-----\\n\",\n        \"signCert\":\"-----BEGIN CERTIFICATE-----\\nMIICKTCCAc+gAwIBAgIRAOdXYQTbaGTp2CfySyHxkSYwCgYIKoZIzj0EAwIwczEL\\nMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBG\\ncmFuY2lzY28xGTAXBgNVBAoTEG9yZzIuZXhhbXBsZS5jb20xHDAaBgNVBAMTE2Nh\\nLm9yZzIuZXhhbXBsZS5jb20wHhcNMjEwOTIyMDkyNDU5WhcNMzEwOTIwMDkyNDU5\\nWjBqMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMN\\nU2FuIEZyYW5jaXNjbzENMAsGA1UECxMEcGVlcjEfMB0GA1UEAxMWcGVlcjAub3Jn\\nMi5leGFtcGxlLmNvbTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABHF4bEYkCZDv\\nZQHW93vXi/PWfvdUISypyCUoy5Rwz82GPs5aVaBKcXTBEA7rRdKT3O+6/kK6/B7g\\ntZADvsJGDqmjTTBLMA4GA1UdDwEB/wQEAwIHgDAMBgNVHRMBAf8EAjAAMCsGA1Ud\\nIwQkMCKAIEZTQmuwfbLNg1BIrHVIBtydnKmK7t4nrORQFLTSlp2dMAoGCCqGSM49\\nBAMCA0gAMEUCIQDQnhyv1w7DaINJRaAgNfKu5f7GZspDcsqs9SgsyQGnJQIgAJzs\\nnF2tAMA1WNSduIevyau0RR5kiKis2c4w+f87fnw=\\n-----END CERTIFICATE-----\\n\",\n        \"node_version\":\"1.1.0\"\n      },\n      \"name\":\"peer0.org2.example.com:9051\",\n      \"peerLocation\":\"grpcs://peer0.org2.example.com:9051\"\n    },\n    {\n      \"peerProperties\":{\n        \"ssl-target-name-override\":\"peer0.org1.example.com\",\n        \"pemBytes\":\"-----BEGIN CERTIFICATE-----\\nMIICSTCCAe+gAwIBAgIQHZi+WcoBbQxEPhphIq0RbzAKBggqhkjOPQQDAjB2MQsw\\nCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNU2FuIEZy\\nYW5jaXNjbzEZMBcGA1UEChMQb3JnMS5leGFtcGxlLmNvbTEfMB0GA1UEAxMWdGxz\\nY2Eub3JnMS5leGFtcGxlLmNvbTAeFw0yMTA5MjIwOTI0NTlaFw0zMTA5MjAwOTI0\\nNTlaMHYxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQH\\nEw1TYW4gRnJhbmNpc2NvMRkwFwYDVQQKExBvcmcxLmV4YW1wbGUuY29tMR8wHQYD\\nVQQDExZ0bHNjYS5vcmcxLmV4YW1wbGUuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0D\\nAQcDQgAEmGkYtSwYOowx6gz4Fl+G03MZy/PS69nOySJBEFgqGUjcYVn2dn6TQY2j\\nWuD0PZGN9edcX1OOx2sgeESxADYQWKNfMF0wDgYDVR0PAQH/BAQDAgGmMA8GA1Ud\\nJQQIMAYGBFUdJQAwDwYDVR0TAQH/BAUwAwEB/zApBgNVHQ4EIgQgEq0ojsQ+567S\\nwOMhK5GoKCxbcXNTmZAY9RiPif/k5GIwCgYIKoZIzj0EAwIDSAAwRQIhAI3JaWgj\\npPXBve7MfgH0Afei57g8bzkGCEOl2z97xwTKAiBVRSFfEZjDBd8dwRS43wz3qsl4\\nXr5ZSfj1zPVQEgshxw==\\n-----END CERTIFICATE-----\\n\",\n        \"signCert\":\"-----BEGIN CERTIFICATE-----\\nMIICKTCCAc+gAwIBAgIRAMNJF0x6ycd9uv+7MpTiTiIwCgYIKoZIzj0EAwIwczEL\\nMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBG\\ncmFuY2lzY28xGTAXBgNVBAoTEG9yZzEuZXhhbXBsZS5jb20xHDAaBgNVBAMTE2Nh\\nLm9yZzEuZXhhbXBsZS5jb20wHhcNMjEwOTIyMDkyNDU5WhcNMzEwOTIwMDkyNDU5\\nWjBqMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMN\\nU2FuIEZyYW5jaXNjbzENMAsGA1UECxMEcGVlcjEfMB0GA1UEAxMWcGVlcjAub3Jn\\nMS5leGFtcGxlLmNvbTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABNmJoivhQ7iA\\nWVTYnwgQAp0K+LSFDHLorOBlxcghYGOxJtqXdzkG/v9S7jCMiCPwPM5aKZC9R7pF\\nqfH4JwIutFujTTBLMA4GA1UdDwEB/wQEAwIHgDAMBgNVHRMBAf8EAjAAMCsGA1Ud\\nIwQkMCKAIM92yvhPXCN9KKEQA0CkTQD/spKx9JoQ43l7GHaJMTrWMAoGCCqGSM49\\nBAMCA0gAMEUCIQCV18hs/WpgF2Y9OgnSVdBLTOEPHSJ2DK/LQsnYMZl6twIgKcyy\\nBUnv5jEGGaPsn2idA63oEMs17z7thlmztmioIKI=\\n-----END CERTIFICATE-----\\n\",\n        \"node_version\":\"1.1.0\"\n      },\n      \"name\":\"peer0.org1.example.com:7051\",\n      \"peerLocation\":\"grpcs://peer0.org1.example.com:7051\"\n    }\n  ],\n  \"channel\":{\n    \"name\":\"mychannel\"\n  },\n  \"chaincode\":{\n    \"path\":\"\",\n    \"name\":\"odatscrosschaincc5\",\n    \"version\":\"\"\n  },\n  \"runningtls\":\"true\",\n  \"orderers\":[\n    {\n      \"ordererLocation\":\"grpcs://orderer.example.com:7050\",\n      \"ordererProperties\":{\n        \"ssl-target-name-override\":\"orderer.example.com\",\n        \"pemBytes\":\"-----BEGIN CERTIFICATE-----\\nMIICNDCCAdugAwIBAgIQfB42TZ8h09MvYpt4d45w5TAKBggqhkjOPQQDAjBsMQsw\\nCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNU2FuIEZy\\nYW5jaXNjbzEUMBIGA1UEChMLZXhhbXBsZS5jb20xGjAYBgNVBAMTEXRsc2NhLmV4\\nYW1wbGUuY29tMB4XDTIxMDkyMjA5MjQ1OVoXDTMxMDkyMDA5MjQ1OVowbDELMAkG\\nA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBGcmFu\\nY2lzY28xFDASBgNVBAoTC2V4YW1wbGUuY29tMRowGAYDVQQDExF0bHNjYS5leGFt\\ncGxlLmNvbTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABOOhl0hG7sZUSzEzNU8X\\n5i2BaDb6cXqM2HENrklATznP6neWpDmzl467rn/bk2vCDZcDoaM25h8ACAmaiq0o\\nAlSjXzBdMA4GA1UdDwEB/wQEAwIBpjAPBgNVHSUECDAGBgRVHSUAMA8GA1UdEwEB\\n/wQFMAMBAf8wKQYDVR0OBCIEIFI1xPFjd2MWNoIfgV2nb8t+lDKETh2/leUCeDC3\\nDxluMAoGCCqGSM49BAMCA0cAMEQCICQZOF6rRMEWA/v2AkRTPUoplfEmmyt0Fb9T\\nzb00cM+CAiBcy76S2mXgF8wxg3wdsIVOdWIYVtWbClz0LzF+7A+prw==\\n-----END CERTIFICATE-----\\n\"\n      },\n      \"name\":\"orderer.example.com:7050\"\n    }\n  ],\n  \"user\":{\n    \"mspId\":\"Org1MSP\",\n    \"name\":\"Admin@org1.example.com\",\n    \"cert\":\"-----BEGIN CERTIFICATE-----\\nMIICKjCCAdGgAwIBAgIRAOHCrua+iIRM7EZv2I2R9YkwCgYIKoZIzj0EAwIwczEL\\nMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBG\\ncmFuY2lzY28xGTAXBgNVBAoTEG9yZzEuZXhhbXBsZS5jb20xHDAaBgNVBAMTE2Nh\\nLm9yZzEuZXhhbXBsZS5jb20wHhcNMjEwOTIyMDkyNDU5WhcNMzEwOTIwMDkyNDU5\\nWjBsMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMN\\nU2FuIEZyYW5jaXNjbzEPMA0GA1UECxMGY2xpZW50MR8wHQYDVQQDDBZBZG1pbkBv\\ncmcxLmV4YW1wbGUuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEnVoxgzBx\\ncCBN84LRWD/4L2WGys1gFVO2ueDSIZ8Y+Yf5dT5T83lZW7z26gQiKhTbe/hztTQt\\na7c7Uu4cvjIRj6NNMEswDgYDVR0PAQH/BAQDAgeAMAwGA1UdEwEB/wQCMAAwKwYD\\nVR0jBCQwIoAgz3bK+E9cI30ooRADQKRNAP+ykrH0mhDjeXsYdokxOtYwCgYIKoZI\\nzj0EAwIDRwAwRAIgGPf+wOB2cctLCH4FILu4MRMVuLx5ixd5cAukC+zFtlMCIAL5\\nSKR2HZSAqbhdd953oj2/xTQGNwOlhLUYBuFqDIU0\\n-----END CERTIFICATE-----\",\n    \"expiration\":\"Sat Sep 20 17:24:59 CST 2031\",\n    \"key\":\"-----BEGIN PRIVATE KEY-----\\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgNQlypdXOVhX1uwmJ\\nIHE6jnv3211BNwrDvqG+zvsUXX2hRANCAASdWjGDMHFwIE3zgtFYP/gvZYbKzWAV\\nU7a54NIhnxj5h/l1PlPzeVlbvPbqBCIqFNt7+HO1NC1rtztS7hy+MhGP\\n-----END PRIVATE KEY-----\"\n  }\n}\n",
	"source":"BAAS-CLOUD"
}
```

你会发现这一部分，共有两个字段，fabric_channel_config和source，fabric_channel_config需要下面额外展开解释，source可以自行定义。
fabric_channel_config是整个fabric channel的网络描述，包含peer节点、orderer等节点信息。
举例如下。

```json
{
	"discoveryPeers":[
		{
			"peerProperties":{
				"ssl-target-name-override":"peer0.org2.example.com",
				"pemBytes":"-----BEGIN CERTIFICATE-----\nMIICSjCCAfCgAwIBAgIRAMCRO4oz1qtN33VV1jiT71MwCgYIKoZIzj0EAwIwdjEL\nMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBG\ncmFuY2lzY28xGTAXBgNVBAoTEG9yZzIuZXhhbXBsZS5jb20xHzAdBgNVBAMTFnRs\nc2NhLm9yZzIuZXhhbXBsZS5jb20wHhcNMjEwOTIyMDkyNDU5WhcNMzEwOTIwMDky\nNDU5WjB2MQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UE\nBxMNU2FuIEZyYW5jaXNjbzEZMBcGA1UEChMQb3JnMi5leGFtcGxlLmNvbTEfMB0G\nA1UEAxMWdGxzY2Eub3JnMi5leGFtcGxlLmNvbTBZMBMGByqGSM49AgEGCCqGSM49\nAwEHA0IABCQzyayGH4H8/bnB31fL/UdAHmgJSFahznbWUJhHNY40njmIYsSsFsW/\nLFyA173AxmOZM0BFnmtCtAvosClglu2jXzBdMA4GA1UdDwEB/wQEAwIBpjAPBgNV\nHSUECDAGBgRVHSUAMA8GA1UdEwEB/wQFMAMBAf8wKQYDVR0OBCIEIAys8XJ2ChOC\neZGuA4f+ovKhaoS14s/jlJfFam4jftUrMAoGCCqGSM49BAMCA0gAMEUCIQCE2Ul4\nU73jHOFeI/Xk2CKrR2gLQM04VV2ime6XM0aJ0gIgeljJRz9eQHlzXPjjxxep70bD\njbLCo7LNPiRGm/UBgzY=\n-----END CERTIFICATE-----\n",
				"node_version":"1.1.0"
			},
			"name":"peer0.org2.example.com:9051",
			"peerLocation":"grpcs://peer0.org2.example.com:9051"
		},
		{
			"peerProperties":{
				"ssl-target-name-override":"peer0.org1.example.com",
				"pemBytes":"-----BEGIN CERTIFICATE-----\nMIICSTCCAe+gAwIBAgIQHZi+WcoBbQxEPhphIq0RbzAKBggqhkjOPQQDAjB2MQsw\nCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNU2FuIEZy\nYW5jaXNjbzEZMBcGA1UEChMQb3JnMS5leGFtcGxlLmNvbTEfMB0GA1UEAxMWdGxz\nY2Eub3JnMS5leGFtcGxlLmNvbTAeFw0yMTA5MjIwOTI0NTlaFw0zMTA5MjAwOTI0\nNTlaMHYxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQH\nEw1TYW4gRnJhbmNpc2NvMRkwFwYDVQQKExBvcmcxLmV4YW1wbGUuY29tMR8wHQYD\nVQQDExZ0bHNjYS5vcmcxLmV4YW1wbGUuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0D\nAQcDQgAEmGkYtSwYOowx6gz4Fl+G03MZy/PS69nOySJBEFgqGUjcYVn2dn6TQY2j\nWuD0PZGN9edcX1OOx2sgeESxADYQWKNfMF0wDgYDVR0PAQH/BAQDAgGmMA8GA1Ud\nJQQIMAYGBFUdJQAwDwYDVR0TAQH/BAUwAwEB/zApBgNVHQ4EIgQgEq0ojsQ+567S\nwOMhK5GoKCxbcXNTmZAY9RiPif/k5GIwCgYIKoZIzj0EAwIDSAAwRQIhAI3JaWgj\npPXBve7MfgH0Afei57g8bzkGCEOl2z97xwTKAiBVRSFfEZjDBd8dwRS43wz3qsl4\nXr5ZSfj1zPVQEgshxw==\n-----END CERTIFICATE-----\n",
				"node_version":"1.1.0"
			},
			"name":"peer0.org1.example.com:7051",
			"peerLocation":"grpcs://peer0.org1.example.com:7051"
		}
	],
	"validatorThreshold":2,
	"validatorPeers":[
		{
			"peerProperties":{
				"ssl-target-name-override":"peer0.org2.example.com",
				"pemBytes":"-----BEGIN CERTIFICATE-----\nMIICSjCCAfCgAwIBAgIRAMCRO4oz1qtN33VV1jiT71MwCgYIKoZIzj0EAwIwdjEL\nMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBG\ncmFuY2lzY28xGTAXBgNVBAoTEG9yZzIuZXhhbXBsZS5jb20xHzAdBgNVBAMTFnRs\nc2NhLm9yZzIuZXhhbXBsZS5jb20wHhcNMjEwOTIyMDkyNDU5WhcNMzEwOTIwMDky\nNDU5WjB2MQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UE\nBxMNU2FuIEZyYW5jaXNjbzEZMBcGA1UEChMQb3JnMi5leGFtcGxlLmNvbTEfMB0G\nA1UEAxMWdGxzY2Eub3JnMi5leGFtcGxlLmNvbTBZMBMGByqGSM49AgEGCCqGSM49\nAwEHA0IABCQzyayGH4H8/bnB31fL/UdAHmgJSFahznbWUJhHNY40njmIYsSsFsW/\nLFyA173AxmOZM0BFnmtCtAvosClglu2jXzBdMA4GA1UdDwEB/wQEAwIBpjAPBgNV\nHSUECDAGBgRVHSUAMA8GA1UdEwEB/wQFMAMBAf8wKQYDVR0OBCIEIAys8XJ2ChOC\neZGuA4f+ovKhaoS14s/jlJfFam4jftUrMAoGCCqGSM49BAMCA0gAMEUCIQCE2Ul4\nU73jHOFeI/Xk2CKrR2gLQM04VV2ime6XM0aJ0gIgeljJRz9eQHlzXPjjxxep70bD\njbLCo7LNPiRGm/UBgzY=\n-----END CERTIFICATE-----\n",
				"signCert":"-----BEGIN CERTIFICATE-----\nMIICKTCCAc+gAwIBAgIRAOdXYQTbaGTp2CfySyHxkSYwCgYIKoZIzj0EAwIwczEL\nMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBG\ncmFuY2lzY28xGTAXBgNVBAoTEG9yZzIuZXhhbXBsZS5jb20xHDAaBgNVBAMTE2Nh\nLm9yZzIuZXhhbXBsZS5jb20wHhcNMjEwOTIyMDkyNDU5WhcNMzEwOTIwMDkyNDU5\nWjBqMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMN\nU2FuIEZyYW5jaXNjbzENMAsGA1UECxMEcGVlcjEfMB0GA1UEAxMWcGVlcjAub3Jn\nMi5leGFtcGxlLmNvbTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABHF4bEYkCZDv\nZQHW93vXi/PWfvdUISypyCUoy5Rwz82GPs5aVaBKcXTBEA7rRdKT3O+6/kK6/B7g\ntZADvsJGDqmjTTBLMA4GA1UdDwEB/wQEAwIHgDAMBgNVHRMBAf8EAjAAMCsGA1Ud\nIwQkMCKAIEZTQmuwfbLNg1BIrHVIBtydnKmK7t4nrORQFLTSlp2dMAoGCCqGSM49\nBAMCA0gAMEUCIQDQnhyv1w7DaINJRaAgNfKu5f7GZspDcsqs9SgsyQGnJQIgAJzs\nnF2tAMA1WNSduIevyau0RR5kiKis2c4w+f87fnw=\n-----END CERTIFICATE-----\n",
				"node_version":"1.1.0"
			},
			"name":"peer0.org2.example.com:9051",
			"peerLocation":"grpcs://peer0.org2.example.com:9051"
		},
		{
			"peerProperties":{
				"ssl-target-name-override":"peer0.org1.example.com",
				"pemBytes":"-----BEGIN CERTIFICATE-----\nMIICSTCCAe+gAwIBAgIQHZi+WcoBbQxEPhphIq0RbzAKBggqhkjOPQQDAjB2MQsw\nCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNU2FuIEZy\nYW5jaXNjbzEZMBcGA1UEChMQb3JnMS5leGFtcGxlLmNvbTEfMB0GA1UEAxMWdGxz\nY2Eub3JnMS5leGFtcGxlLmNvbTAeFw0yMTA5MjIwOTI0NTlaFw0zMTA5MjAwOTI0\nNTlaMHYxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQH\nEw1TYW4gRnJhbmNpc2NvMRkwFwYDVQQKExBvcmcxLmV4YW1wbGUuY29tMR8wHQYD\nVQQDExZ0bHNjYS5vcmcxLmV4YW1wbGUuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0D\nAQcDQgAEmGkYtSwYOowx6gz4Fl+G03MZy/PS69nOySJBEFgqGUjcYVn2dn6TQY2j\nWuD0PZGN9edcX1OOx2sgeESxADYQWKNfMF0wDgYDVR0PAQH/BAQDAgGmMA8GA1Ud\nJQQIMAYGBFUdJQAwDwYDVR0TAQH/BAUwAwEB/zApBgNVHQ4EIgQgEq0ojsQ+567S\nwOMhK5GoKCxbcXNTmZAY9RiPif/k5GIwCgYIKoZIzj0EAwIDSAAwRQIhAI3JaWgj\npPXBve7MfgH0Afei57g8bzkGCEOl2z97xwTKAiBVRSFfEZjDBd8dwRS43wz3qsl4\nXr5ZSfj1zPVQEgshxw==\n-----END CERTIFICATE-----\n",
				"signCert":"-----BEGIN CERTIFICATE-----\nMIICKTCCAc+gAwIBAgIRAMNJF0x6ycd9uv+7MpTiTiIwCgYIKoZIzj0EAwIwczEL\nMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBG\ncmFuY2lzY28xGTAXBgNVBAoTEG9yZzEuZXhhbXBsZS5jb20xHDAaBgNVBAMTE2Nh\nLm9yZzEuZXhhbXBsZS5jb20wHhcNMjEwOTIyMDkyNDU5WhcNMzEwOTIwMDkyNDU5\nWjBqMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMN\nU2FuIEZyYW5jaXNjbzENMAsGA1UECxMEcGVlcjEfMB0GA1UEAxMWcGVlcjAub3Jn\nMS5leGFtcGxlLmNvbTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABNmJoivhQ7iA\nWVTYnwgQAp0K+LSFDHLorOBlxcghYGOxJtqXdzkG/v9S7jCMiCPwPM5aKZC9R7pF\nqfH4JwIutFujTTBLMA4GA1UdDwEB/wQEAwIHgDAMBgNVHRMBAf8EAjAAMCsGA1Ud\nIwQkMCKAIM92yvhPXCN9KKEQA0CkTQD/spKx9JoQ43l7GHaJMTrWMAoGCCqGSM49\nBAMCA0gAMEUCIQCV18hs/WpgF2Y9OgnSVdBLTOEPHSJ2DK/LQsnYMZl6twIgKcyy\nBUnv5jEGGaPsn2idA63oEMs17z7thlmztmioIKI=\n-----END CERTIFICATE-----\n",
				"node_version":"1.1.0"
			},
			"name":"peer0.org1.example.com:7051",
			"peerLocation":"grpcs://peer0.org1.example.com:7051"
		}
	],
	"channel":{
		"name":"mychannel"
	},
	"chaincode":{
		"path":"",
		"name":"odatscrosschaincc5",
		"version":""
	},
	"runningtls":"true",
	"orderers":[
		{
			"ordererLocation":"grpcs://orderer.example.com:7050",
			"ordererProperties":{
				"ssl-target-name-override":"orderer.example.com",
				"pemBytes":"-----BEGIN CERTIFICATE-----\nMIICNDCCAdugAwIBAgIQfB42TZ8h09MvYpt4d45w5TAKBggqhkjOPQQDAjBsMQsw\nCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNU2FuIEZy\nYW5jaXNjbzEUMBIGA1UEChMLZXhhbXBsZS5jb20xGjAYBgNVBAMTEXRsc2NhLmV4\nYW1wbGUuY29tMB4XDTIxMDkyMjA5MjQ1OVoXDTMxMDkyMDA5MjQ1OVowbDELMAkG\nA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBGcmFu\nY2lzY28xFDASBgNVBAoTC2V4YW1wbGUuY29tMRowGAYDVQQDExF0bHNjYS5leGFt\ncGxlLmNvbTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABOOhl0hG7sZUSzEzNU8X\n5i2BaDb6cXqM2HENrklATznP6neWpDmzl467rn/bk2vCDZcDoaM25h8ACAmaiq0o\nAlSjXzBdMA4GA1UdDwEB/wQEAwIBpjAPBgNVHSUECDAGBgRVHSUAMA8GA1UdEwEB\n/wQFMAMBAf8wKQYDVR0OBCIEIFI1xPFjd2MWNoIfgV2nb8t+lDKETh2/leUCeDC3\nDxluMAoGCCqGSM49BAMCA0cAMEQCICQZOF6rRMEWA/v2AkRTPUoplfEmmyt0Fb9T\nzb00cM+CAiBcy76S2mXgF8wxg3wdsIVOdWIYVtWbClz0LzF+7A+prw==\n-----END CERTIFICATE-----\n"
			},
			"name":"orderer.example.com:7050"
		}
	],
	"user":{
		"mspId":"Org1MSP",
		"name":"Admin@org1.example.com",
		"cert":"-----BEGIN CERTIFICATE-----\nMIICKjCCAdGgAwIBAgIRAOHCrua+iIRM7EZv2I2R9YkwCgYIKoZIzj0EAwIwczEL\nMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBG\ncmFuY2lzY28xGTAXBgNVBAoTEG9yZzEuZXhhbXBsZS5jb20xHDAaBgNVBAMTE2Nh\nLm9yZzEuZXhhbXBsZS5jb20wHhcNMjEwOTIyMDkyNDU5WhcNMzEwOTIwMDkyNDU5\nWjBsMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMN\nU2FuIEZyYW5jaXNjbzEPMA0GA1UECxMGY2xpZW50MR8wHQYDVQQDDBZBZG1pbkBv\ncmcxLmV4YW1wbGUuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEnVoxgzBx\ncCBN84LRWD/4L2WGys1gFVO2ueDSIZ8Y+Yf5dT5T83lZW7z26gQiKhTbe/hztTQt\na7c7Uu4cvjIRj6NNMEswDgYDVR0PAQH/BAQDAgeAMAwGA1UdEwEB/wQCMAAwKwYD\nVR0jBCQwIoAgz3bK+E9cI30ooRADQKRNAP+ykrH0mhDjeXsYdokxOtYwCgYIKoZI\nzj0EAwIDRwAwRAIgGPf+wOB2cctLCH4FILu4MRMVuLx5ixd5cAukC+zFtlMCIAL5\nSKR2HZSAqbhdd953oj2/xTQGNwOlhLUYBuFqDIU0\n-----END CERTIFICATE-----",
		"expiration":"Sat Sep 20 17:24:59 CST 2031",
		"key":"-----BEGIN PRIVATE KEY-----\nMIGHAgEAMBMGByqGSM.../l1PlPzeVlbvPbqBCIqFNt7+HO1NC1rtztS7hy+MhGP\n-----END PRIVATE KEY-----"
	}
}
```

如果你是交付同学，想要获得上面的json，则**需要和客户收集这些信息，一般需要让客户填写一个**[**YAML文件**](https://yuque.antfin.com/docs/share/f88008b8-edd8-4f9b-b4a3-9516491b5bf6?) **，**然后提交给开发人员，转换为json，当然有控制台界面则不需要。下面解释一下这些字段：

| 字段                     | 解释                                                   |
| ------------------------ | ------------------------------------------------------ |
| discoveryPeers           | peer节点信息的数组                                     |
| peerProperties           | peer节点的属性信息                                     |
| ssl-target-name-override | 在建立TLS连接时，是否要覆盖目标名字                    |
| pemBytes                 | peer节点的SSL证书                                      |
| node_version             | 节点版本，若为1.1，则必须明确版本，默认1.4             |
| name                     | 节点的名字                                             |
| peerLocation             | peer节点的URL                                          |
| validatorThreshold       | 至少N个validator对某消息做出背书，中继才会信任该消息。 |
| validatorPeers           | 用于背书的peer节点信息                                 |
| signCert                 | peer节点MSP中的身份证书                                |
| channel                  | 当前channel的信息                                      |
| name                     | 当前channel的名字                                      |
| chaincode                | 跨链链码信息，一般只填name部分                         |
| runningtls               | 一般填true，当peer节点未开启tls的时候则使用false       |
| orderers                 | 排序节点的信息                                         |
| ordererLocation          | 排序节点的URL                                          |
| ordererProperties        | 排序节点的属性信息，pemBytes是节点的SSL证书            |
| user                     | 该部分是中继的Fabric账户信息                           |
| mspId                    | 该账户的MSP ID                                         |
| name                     | 该账户的名字                                           |
| cert                     | 该账户的身份证书                                       |
| key                      | 该账户的身份私钥                                       |



##### 5. 启动中继扫描服务

将区块链的网络信息配置到中继之后，就可以启动中继对这条链的监听服务。

```
odats>blockchainManager.startBlockchianAnchor("mychain_0.10", "YOUR_ID")
```

一样地，第一个参数为区块链的类型；第二个为区块链ID，和上几步相同；
成功运行后，会有“startBlockchianAnchor success.”出现。
​

##### 6. 部署合约

下面让中继为注册的链部署跨链必须的智能合约，称为跨链合约。
首先部署OracleService：

```
odats>serviceManager.deployOracleServiceAsync("mychain_0.10", "YOUR_ID")
```

然后部署AMService:

```
odats>serviceManager.deployAMServiceAsync("mychain_0.10", "YOUR_ID")
```

一样地，第一个参数为区块链的类型；第二个为区块链ID，和上几步相同；


完成之后，因为是异步部署，可能需要等待一段时间，如果账户信息等存在问题则可能会报错。
要查看部署情况，可以看数据库：

```sql
SELECT CONVERT(properties using utf8) FROM `blockchain` WHERE `blockchain_id` = "YOUR_ID";
```

这样会返回一个json，观察其字段

- 【am_service_status】应该是FINISH_DEPLOY_AM_CONTRACT
- 【oracle_service_status】应该是FINISH_DEPLOY_SERVICE


#### CLI停止区块链
如果区块链不再有跨链需求，则可以让中继停止监听该链。
```
odats>blockchainManager.stopBlockchianAnchor("mychain_0.10", "YOUR_ID")
```
如此，可停止该链。