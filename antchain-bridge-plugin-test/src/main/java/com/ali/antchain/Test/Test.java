package com.ali.antchain.Test;

import java.lang.reflect.Method;

public class Test {
    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        Class<?> server = Class.forName("com.alipay.antchain.bridge.pluginserver.server.PluginManagementServiceImpl");
        Object obj = server.newInstance();

        Method method = server.getMethod("getPluginInfo", String.class);


    }
}
