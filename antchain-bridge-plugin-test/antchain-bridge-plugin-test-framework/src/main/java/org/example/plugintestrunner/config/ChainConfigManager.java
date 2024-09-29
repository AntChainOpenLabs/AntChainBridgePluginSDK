package org.example.plugintestrunner.config;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

public class ChainConfigManager {

    private static ChainConfigManager instance;
    private final Properties config;

    // 私有构造函数
    private ChainConfigManager() {
        config = new Properties();
        loadProperties();
    }

    // 公共方法，用于获取单例实例
    public static synchronized ChainConfigManager getInstance() {
        if (instance == null) {
            instance = new ChainConfigManager();
        }
        return instance;
    }

    private void loadProperties() {
        try (InputStream input = ChainConfigManager.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return;
            }
            // 加载properties文件
            config.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String getProperty(String key) {
        return config.getProperty(key);
    }
}