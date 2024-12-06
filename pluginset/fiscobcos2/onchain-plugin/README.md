<div align="center">
  <img alt="am logo" src="https://gw.alipayobjects.com/zos/bmw-prod/3ee4adc7-1960-4dbf-982e-522ac135a0c0.svg" width="250" >
  <h1 align="center">AntChain Bridge Fisco2.*插件系统合约库</h1>
  <p align="center">
    <a href="http://makeapullrequest.com">
      <img alt="pull requests welcome badge" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat">
    </a>
  </p>
</div>

fisco2.*控制台编译器默认支持0.4.25版本solidity。

fisco-java-sdk在支持bytes32类型上存在缺陷，使用java代码调用合约时请注意：
- 如遇bytes类型或bytesN类型参数，需要传入byte[]类型参数；
- 如遇string类型参数，直接传入String类型参数而非Utf8String类型。

此外，合约返回数字时可能会返回带有`0x`前缀的十六进制数字字符串。