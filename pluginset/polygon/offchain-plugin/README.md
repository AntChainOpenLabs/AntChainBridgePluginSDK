<div align="center">
  <img alt="am logo" src="https://gw.alipayobjects.com/zos/bmw-prod/3ee4adc7-1960-4dbf-982e-522ac135a0c0.svg" width="250" >
  <h1 align="center">Polygon PoS Plugin</h1>
  <p align="center">
    <a href="http://makeapullrequest.com">
      <img alt="pull requests welcome badge" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat">
    </a>
  </p>
</div>


# Build

1. Before compiling the plugin, you can modify the type and id of the plugin by modifying 
the `products`  and `pluginId`  attribute values in `PolygonBBCService.java` .
In the current code, the product is `polygon` , and the pluginId is `plugin-polygon`.

```java
@BBCService(products = "polygon", pluginId = "plugin-polygon")
```

2. Then execute the compile command in the plugin project directory 
to get the jar for use as a plugin
```agsl
mvn clean package -Dmaven.test.skip=true
```

3. The Polygon PoS plugin use same contracts with ethereum plugin, please [check](../../ethereum/onchain-plugin).
4. Here is the configuration example：

```json
{
	"gasLimit":3000000,
	"gasPrice":2300000000,
	"privateKey":"74edbcb88fa5568...942bd4becca9c",
	"url":"https://rpc-amoy.polygon.technology/"
}
```

