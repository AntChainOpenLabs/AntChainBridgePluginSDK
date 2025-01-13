/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.pluginserver.cli.command;

import java.util.HashMap;
import java.util.Map;

public class CommandNamespaceImpl implements CommandNamespace {
    /**
     * 命令集合
     */
    private final Map<String, Command> commands = new HashMap<>();

    /**
     * 命名空间名称
     *
     * @return
     */
    @Override
    public String name() {
        return null;
    }

    /**
     * 往命名空间添加一个命令
     *
     * @param cmd
     */
    public void addCommand(Command cmd) {
        commands.put(cmd.getName(), cmd);
    }

    /**
     * 获取命名空间下所有命令
     *
     * @return
     */
    public Map<String, Command> getCommands() {
        return commands;
    }
}
