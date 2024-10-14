package com.alipay.antchain.bridge.plugintestrunner.testcase;

import com.alipay.antchain.bridge.plugintestrunner.exception.TestCaseException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;
import com.alipay.antchain.bridge.plugintestrunner.exception.TestCaseException.*;

import java.lang.reflect.Field;
import java.util.*;


@JsonDeserialize(using = TestCaseDeserializer.class)
@Setter
@Getter
public class TestCase {
    // 从 testcase.json 中读取的配置信息
    private String name;
    private String jarPath;
    private String product;
    private String domain;
    private TestCaseChainConf chainConf;
    private List<String> pluginLoadAndStartTestList;
    private List<String> pluginInterfaceTestList;

    // 判断链配置是否合法
    @JsonIgnore
    private boolean chainConfValid;

    // 插件加载启动测试标志
    @JsonIgnore
    private final List<String> ValidPluginLoadAndStartTestList = Arrays.asList("loadPlugin", "startPlugin", "startPluginFromStop", "stopPlugin", "createBBCService");
    @JsonIgnore
    private boolean loadPlugin;
    @JsonIgnore
    private boolean startPlugin;
    @JsonIgnore
    private boolean startPluginFromStop;
    @JsonIgnore
    private boolean stopPlugin;
    @JsonIgnore
    private boolean createBBCService;

    // 插件测试标志
    @JsonIgnore
    private final List<String> ValidPluginInterfaceTestList = Arrays.asList("startup", "shutdown", "getContext", "queryLatestHeight", "setupAuthMessageContract", "setupSDPMessageContract", "setLocalDomain", "querySDPMessageSeq", "setProtocol", "setAmContract", "readCrossChainMessagesByHeight", "relayAuthMessage", "readCrossChainMessageReceipt");
    @JsonIgnore
    private boolean startup;
    @JsonIgnore
    private boolean isStartupSuccess;

    @JsonIgnore
    private boolean shutdown;
    @JsonIgnore
    private boolean isShutdownSuccess;

    @JsonIgnore
    private boolean getContext;
    @JsonIgnore
    private boolean isGetContextSuccess;

    @JsonIgnore
    private boolean queryLatestHeight;
    @JsonIgnore
    private boolean isQueryLatestHeightSuccess;

    @JsonIgnore
    private boolean setupAuthMessageContract;
    @JsonIgnore
    private boolean isSetupAuthMessageContractSuccess;

    @JsonIgnore
    private boolean setupSDPMessageContract;
    @JsonIgnore
    private boolean isSetupSDPMessageContractSuccess;

    @JsonIgnore
    private boolean setLocalDomain;
    @JsonIgnore
    private boolean isSetLocalDomainSuccess;

    @JsonIgnore
    private boolean querySDPMessageSeq;
    @JsonIgnore
    private boolean isQuerySDPMessageSeqSuccess;

    @JsonIgnore
    private boolean setProtocol;
    @JsonIgnore
    private boolean isSetProtocolSuccess;

    @JsonIgnore
    private boolean setAmContract;
    @JsonIgnore
    private boolean isSetAmContractSuccess;

    @JsonIgnore
    private boolean readCrossChainMessagesByHeight;
    @JsonIgnore
    private boolean isReadCrossChainMessagesByHeightSuccess;

    @JsonIgnore
    private boolean relayAuthMessage;
    @JsonIgnore
    private boolean isRelayAuthMessageSuccess;

    @JsonIgnore
    private boolean readCrossChainMessageReceipt;
    @JsonIgnore
    private boolean isReadCrossChainMessageReceiptSuccess;

    // testcase 执行结果
    @JsonIgnore
    private boolean pluginLoadAndStartTestSuccess;
    @JsonIgnore
    private boolean pluginInterfaceTestSuccess;

    // 检查 chainConf 合法性，合法返回 true
    public boolean isChainConfValid() {
        chainConfValid = chainConf != null && chainConf.isValid();
        return chainConfValid;
    }

    // 检查 pluginLoadAndStartTestList 合法性
    public void checkPluginLoadAndStartTestList() throws TestCaseException {
        List<String> invalidFields = new ArrayList<>();
        for (String item : pluginLoadAndStartTestList) {
            if (!ValidPluginLoadAndStartTestList.contains(item)) {
                invalidFields.add(item);
            }
        }
        if (!invalidFields.isEmpty()) {
            throw new InValidFieldException("Invalid Field: " + invalidFields);
        }
    }

    // 检查 pluginInterfaceTestList 合法性
    public void checkPluginInterfaceTestList() throws TestCaseException {
        List<String> invalidFields = new ArrayList<>();
        for (String item : pluginInterfaceTestList) {
            if (!ValidPluginInterfaceTestList.contains(item)) {
                invalidFields.add(item);
            }
        }
        if (!invalidFields.isEmpty()) {
            throw new InValidFieldException("Invalid Field: " + invalidFields);
        }
    }

    public void setPluginInterfaceTestFlag() throws TestCaseException {
        checkPluginInterfaceTestList();
        startup = pluginInterfaceTestList.contains("startup");
        shutdown = pluginInterfaceTestList.contains("shutdown");
        getContext = pluginInterfaceTestList.contains("getContext");
        queryLatestHeight = pluginInterfaceTestList.contains("queryLatestHeight");
        setupAuthMessageContract = pluginInterfaceTestList.contains("setupAuthMessageContract");
        setupSDPMessageContract = pluginInterfaceTestList.contains("setupSDPMessageContract");
        setLocalDomain = pluginInterfaceTestList.contains("setLocalDomain");
        querySDPMessageSeq = pluginInterfaceTestList.contains("querySDPMessageSeq");
        setProtocol = pluginInterfaceTestList.contains("setProtocol");
        setAmContract = pluginInterfaceTestList.contains("setAmContract");
        readCrossChainMessagesByHeight = pluginInterfaceTestList.contains("readCrossChainMessagesByHeight");
        relayAuthMessage = pluginInterfaceTestList.contains("relayAuthMessage");
        readCrossChainMessageReceipt = pluginInterfaceTestList.contains("readCrossChainMessageReceipt");
    }

    public HashMap<String, List<String>> getPluginInterfaceTestDependency() {
        HashMap<String, List<String>> map = new HashMap<>();
        map.put("startup", null);
        map.put("shutdown", Collections.singletonList("startup"));
        map.put("getContext", Collections.singletonList("startup"));
        map.put("queryLatestHeight", Collections.singletonList("startup"));
        map.put("setupAuthMessageContract", Arrays.asList("startup", "testGetContext"));
        map.put("setupSDPMessageContract", Arrays.asList("startup", "testGetContext"));
        map.put("setLocalDomain", Arrays.asList("startup", "getContext", "setupSDPMessageContract"));
        map.put("querySDPMessageSeq", Arrays.asList("startup", "getContext", "setupSDPMessageContract", "setLocalDomain"));
        map.put("setProtocol", Arrays.asList("startup", "getContext", "setupAuthMessageContract", "setupSDPMessageContract"));
        map.put("setAmContract", Arrays.asList("startup", "getContext", "setupAuthMessageContract", "setupSDPMessageContract"));
        map.put("readCrossChainMessagesByHeight", Arrays.asList("startup", "getContext", "setupAuthMessageContract", "setupSDPMessageContract", "setProtocol", "setAmContract", "setLocalDomain"));
        map.put("relayAuthMessage", Arrays.asList("startup", "getContext", "setupAuthMessageContract", "setupSDPMessageContract", "setProtocol", "setAmContract", "setLocalDomain"));
        map.put("readCrossChainMessageReceipt", Arrays.asList("startup", "getContext", "setupAuthMessageContract", "setupSDPMessageContract", "setProtocol", "relayAuthMessage", "setAmContract", "setLocalDomain"));
        return map;
    }

    public void setPluginLoadAndStartTestFlag() throws TestCaseException {
        checkPluginLoadAndStartTestList();
        loadPlugin = pluginLoadAndStartTestList.contains("loadPlugin");
        startPlugin = pluginLoadAndStartTestList.contains("startPlugin");
        startPluginFromStop = pluginLoadAndStartTestList.contains("startPluginFromStop");
        stopPlugin = pluginLoadAndStartTestList.contains("stopPlugin");
        createBBCService = pluginLoadAndStartTestList.contains("createBBCService");
    }

    public Object getFieldValue(String fieldName) throws IllegalAccessException, NoSuchFieldException {
        // 获取对象的Class对象
        Class<?> clazz = this.getClass();

        // 获取指定字段
        Field field = clazz.getDeclaredField(fieldName);

        // 设置可访问性，允许访问private字段
        field.setAccessible(true);

        // 返回字段的值
        return field.get(this);
    }
}
