# Fabric CrossChain 系统链码

## Package
如果使用`peer lifecycle chaincode package`打包的话，提前执行下面的操作：

- 选择版本，如果是v2.x则使用v2.2，反之v1.4

- 将vendor和go.mod拷贝到v2.2或者v1.4下面

```
cp -r ./vendor ./v2.2
cp -r ./go.mod ./v2.2
```

- 执行`peer lifecycle chaincode package`
```
peer lifecycle chaincode package odatscrosschaincc.1.6.0.tar.gz --path ./v2.2 --lang golang --label odatscrosschaincc_1.6.0
```