plugin-manager load -j plugin-testchain-0.1-SNAPSHOT-plugin.jar
plugin-manager start -j plugin-testchain-0.1-SNAPSHOT-plugin.jar
plugin-manager load -j invalid-plugin-testchain-0.1-SNAPSHOT-plugin.jar

chain-manager start -p simple-ethereum
chain-manager stop -p simple-ethereum