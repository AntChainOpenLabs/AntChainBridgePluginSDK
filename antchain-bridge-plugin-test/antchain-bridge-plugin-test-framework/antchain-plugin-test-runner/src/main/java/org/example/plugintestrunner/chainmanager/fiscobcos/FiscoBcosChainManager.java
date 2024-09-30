package org.example.plugintestrunner.chainmanager.fiscobcos;

import lombok.Getter;
import org.example.plugintestrunner.chainmanager.IChainManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
public class FiscoBcosChainManager extends IChainManager {

    private String ca_cert;
    private String ssl_cert;
    private String ssl_key;
    private String group_id;
    

    public FiscoBcosChainManager(String confDir) throws IOException {
        this.ca_cert = readFile(Paths.get(confDir, "ca.crt"));
        this.ssl_cert = readFile(Paths.get(confDir, "sdk.crt"));
        this.ssl_key = readFile(Paths.get(confDir, "sdk.key"));
        this.group_id = "group0";
        this.config = String.format("{\"caCert\":\"%s\",\"sslCert\":\"%s\",\"sslKey\":\"%s\",\"groupID\":\"%s\"}",
                this.ca_cert, this.ssl_cert, this.ssl_key, this.group_id);
    }

    private String readFile(Path path) throws IOException {
        String ret = "";
        ret = new String(Files.readAllBytes(path));
        return ret;
    }

    @Override
    public void close() {

    }
}
