<div align="center">
  <img alt="am logo" src="https://gw.alipayobjects.com/zos/bmw-prod/3ee4adc7-1960-4dbf-982e-522ac135a0c0.svg" width="250" >
  <h1 align="center">AntChain Bridge Mychain插件系统合约库</h1>
  <p align="center">
    <a href="http://makeapullrequest.com">
      <img alt="pull requests welcome badge" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat">
    </a>
  </p>
</div>

## Mychain BBC系统合约（solidity）
合约编译依赖`0.4.24`版本的[`solc`编译器](https://antdigital.com/docs/11/101793#h2--solc-5:~:text=%E4%BD%BF%E7%94%A8%E8%AF%B4%E6%98%8E%E3%80%82-,%E4%BA%8C%E8%BF%9B%E5%88%B6%C2%A0solc%C2%A0%E7%BC%96%E8%AF%91%E5%B7%A5%E5%85%B7,-solc%2Djs%C2%A0%E7%BC%96%E8%AF%91)
和[`solc-js`编译器](https://antdigital.com/docs/11/101793#h2--solc-js1:~:text=%E8%BF%9B%E8%A1%8C%E7%AE%80%E8%A6%81%E8%AF%B4%E6%98%8E%E3%80%82-,%E4%B8%8B%E8%BD%BD%C2%A0solc%2Djs,-%E5%8D%95%E5%87%BB%E6%AD%A4%E5%A4%84)

执行`onchain-plugin/src`目录下的`compile_evm_all.sh`脚本可以编译合约，
并将编译生成的`*.bin`（用于合约部署）和`*_runtime.bin`（用于合约升级）
自动更新到`offchain-plugin/src/main/resources/contract/1.5.0/solidity`目录

## Mychain BBC系统合约（c++）

合约普通编译依赖[`0.10.2.7.1（336eb50）`版本的`my++`编译器](https://antdigital.com/docs/11/426717)，
合约JIT编译（合约性能更好）依赖[`2.24`版本的`my++`编译器](https://antdigital.com/docs/11/426685)