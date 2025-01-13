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

package com.alipay.antchain.bridge.pluginserver.cli.shell;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Stream;

import cn.hutool.core.collection.ListUtil;
import com.alipay.antchain.bridge.pluginserver.cli.command.ArgsConstraint;
import com.alipay.antchain.bridge.pluginserver.cli.command.Command;
import com.alipay.antchain.bridge.pluginserver.cli.command.CommandNamespaceImpl;
import com.alipay.antchain.bridge.pluginserver.managementservice.PluginManageRequest;
import com.google.common.collect.Lists;

public abstract class GroovyScriptCommandNamespace extends CommandNamespaceImpl {

    private static final String COMMAND_NAMESPACE_NAME = "name";

    /**
     * 命名空间名称由子类实现
     *
     * @return
     */
    @Override
    public abstract String name();

    public GroovyScriptCommandNamespace() {
        super();
        loadCommand();
    }

    /**
     * 初始化:加载command,默认将子类所有方法解析为命令
     */
    public void loadCommand() {

        Method[] methods = this.getClass().getDeclaredMethods();

        Stream.of(methods).forEach(method -> {

            if (COMMAND_NAMESPACE_NAME.equals(method.getName())) {
                return;
            }

            Command cmd = new Command(method.getName());

            Parameter[] params = method.getParameters();

            for (Parameter param : params) {

                String argName = param.getName();
                List<String> constraints = Lists.newArrayList();

                ArgsConstraint argsConstraint = param.getAnnotation(ArgsConstraint.class);

                if (null != argsConstraint) {
                    if (null != argsConstraint.name() && !"".equals(argsConstraint.name().trim())) {
                        argName = argsConstraint.name().trim();
                    }
                    if (null != argsConstraint.constraints()) {
                        Stream.of(argsConstraint.constraints()).filter(
                                constraint -> null != constraint && !"".equals(constraint.trim())).forEach(
                                constraint -> constraints.add(constraint));
                    }
                }

                cmd.addArgs(argName, param.getType().getSimpleName(), constraints);
            }
            addCommand(cmd);
        });
    }

    protected String queryAPI(String command, Object... args) {

        if (args != null) {
            String[] strArgs = new String[args.length];
            for (int i = 0; i < args.length; ++i) {
                strArgs[i] = args[i].toString();
            }

            return queryAPI(command, strArgs);
        } else {

            return queryAPI(command);
        }
    }

    /**
     * 查询api,供子类命令执行使用
     *
     * @param command
     * @param args
     * @return
     */
    protected String queryAPI(String command, String... args) {

        switch (command) {
            case "loadPlugins":
                return Shell.RUNTIME.getGrpcClient().managePlugin(
                        PluginManageRequest.Type.LOAD_PLUGINS,
                        "", ""
                );
            case "startPlugins":
                return Shell.RUNTIME.getGrpcClient().managePlugin(
                        PluginManageRequest.Type.START_PLUGINS,
                        "", ""
                );
            case "reloadPlugin":
                if (args.length != 1) {
                    return "wrong length of arguments";
                }
                return Shell.RUNTIME.getGrpcClient().managePlugin(
                        PluginManageRequest.Type.RELOAD_PLUGIN,
                        args[0], ""
                );
            case "reloadPluginInNewPath":
                if (args.length != 2) {
                    return "wrong length of arguments";
                }
                return Shell.RUNTIME.getGrpcClient().managePlugin(
                        PluginManageRequest.Type.RELOAD_PLUGIN_IN_NEW_PATH,
                        args[0], args[1]
                );
            case "startPlugin":
                if (args.length != 1) {
                    return "wrong length of arguments";
                }
                return Shell.RUNTIME.getGrpcClient().managePlugin(
                        PluginManageRequest.Type.START_PLUGIN,
                        "", args[0]
                );
            case "stopPlugin":
                if (args.length != 1) {
                    return "wrong length of arguments";
                }
                return Shell.RUNTIME.getGrpcClient().managePlugin(
                        PluginManageRequest.Type.STOP_PLUGIN,
                        args[0], ""
                );
            case "loadPlugin":
                if (args.length != 1) {
                    return "wrong length of arguments";
                }
                return Shell.RUNTIME.getGrpcClient().managePlugin(
                        PluginManageRequest.Type.LOAD_PLUGIN,
                        "", args[0]
                );
            case "startPluginFromStop":
                if (args.length != 1) {
                    return "wrong length of arguments";
                }
                return Shell.RUNTIME.getGrpcClient().managePlugin(
                        PluginManageRequest.Type.START_PLUGIN_FROM_STOP,
                        args[0], ""
                );
            case "hasPlugins":
                if (args.length == 0) {
                    return "zero arguments";
                }
                return Shell.RUNTIME.getGrpcClient().hasPlugins(ListUtil.toList(args));
            case "allPlugins":
                if (args.length != 0) {
                    return "wrong length of arguments";
                }
                return Shell.RUNTIME.getGrpcClient().getAllPlugins();
            case "hasDomains":
                if (args.length == 0) {
                    return "zero arguments";
                }
                return Shell.RUNTIME.getGrpcClient().hasDomains(ListUtil.toList(args));
            case "allDomains":
                if (args.length != 0) {
                    return "wrong length of arguments";
                }
                return Shell.RUNTIME.getGrpcClient().getAllDomains();
            case "restartBBC":
                if (args.length != 2) {
                    return "wrong length of arguments";
                }
                return Shell.RUNTIME.getGrpcClient().restartBBC(args[0], args[1]);
            default:
                return "wrong command " + command;
        }
    }

    protected void print(String result) {
        Shell.RUNTIME.getPrinter().println(result);
    }
}
