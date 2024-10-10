package org.example.plugintestrunner.chainmanager.fabric;

import lombok.Getter;
import org.example.plugintestrunner.chainmanager.IChainManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Getter
public class FabricChainManager extends IChainManager {

    public FabricChainManager(String conf_file) {
        StringBuilder jsonStringBuilder = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(conf_file), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read chainmaker JSON file from path: " + conf_file, e);
        }
        this.config = jsonStringBuilder.toString();
    }

    @Override
    public void close() {

    }
}
