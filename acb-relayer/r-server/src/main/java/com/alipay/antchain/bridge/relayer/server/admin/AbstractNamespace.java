/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.antchain.bridge.relayer.server.admin;

import java.util.HashMap;
import java.util.Map;

import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractNamespace implements Namespace {

    private final Map<String, CommandHandler> commands = new HashMap<>();

    /**
     * 执行命令
     *
     * <p>执行结果为一个pojo对象,最终会被框架序列化为json返回给调用方,实现该方法需要注意这一点。</p>
     *
     * @param command
     * @param args
     * @return result
     */
    @Override
    public Object executeCommand(String command, String... args) {

        if (!commands.containsKey(command)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.SERVER_ADMIN_COMMAND_NOT_EXIST,
                    "cmd {} not exist",
                    command
            );
        }

        try {
            return commands.get(command).execute(args);
        } catch (Exception e) {
            log.error("process admin request {} failed:", command, e);
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.SERVER_ADMIN_UNEXPECTED_ERROR,
                    e,
                    "failed to process command {} with args {}",
                    command, args
            );
        }
    }

    public void addCommand(String name, CommandHandler commandHandler) {
        this.commands.put(name, commandHandler);
    }
}
