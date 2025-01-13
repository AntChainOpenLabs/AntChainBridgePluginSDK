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

import java.io.*;
import java.nio.charset.Charset;

import cn.hutool.core.io.resource.ResourceUtil;
import com.alipay.antchain.bridge.pluginserver.cli.command.NamespaceManager;
import com.alipay.antchain.bridge.pluginserver.cli.command.NamespaceManagerImpl;
import com.alipay.antchain.bridge.pluginserver.cli.core.ManagementGrpcClient;
import org.apache.commons.cli.*;

public class Launcher {
    private static final String OP_HELP = "h";
    private static final String OP_VERSION = "v";
    private static final String OP_PORT = "p";
    private static final String OP_CMD = "c";
    private static final String OP_FILE = "f";

    private static final String OP_HOST = "H";

    private static final Options options;

    static {
        options = new Options();
        options.addOption(OP_HELP, "help", false, "print help info");
        options.addOption(OP_VERSION, "version", false, "print version info");
        options.addOption(OP_PORT, "port", true, "management server port");
        options.addOption(OP_CMD, "command", true, "execute the command");
        options.addOption(OP_FILE, "file", true, "execute multiple commands in the file");
        options.addOption(OP_HOST, "host", true, "set the host");
    }

    public static void main(String[] args) throws ParseException {

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(OP_HELP)) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("plugin server CLI", options);
            return;
        }

        if (cmd.hasOption(OP_VERSION)) {
            System.out.printf("cli version : %s\n", getVersion());
            return;
        }

        int port = 9091;
        if (cmd.hasOption(OP_PORT)) {
            port = Integer.parseInt(cmd.getOptionValue(OP_PORT));
        }

        // new namespace
        NamespaceManager namespaceManager = new NamespaceManagerImpl();

        // new shellProvider
        ShellProvider shellProvider = new GroovyShellProvider(namespaceManager);

        // new promptCompleter
        PromptCompleter promptCompleter = new PromptCompleter();
        promptCompleter.addNamespace(namespaceManager);

        // new grpcClient
        String host = "localhost";
        if (cmd.hasOption(OP_HOST)) {
            host = cmd.getOptionValue(OP_HOST);
        }
        ManagementGrpcClient grpcClient = new ManagementGrpcClient(host, port);

        if (!grpcClient.checkServerStatus()) {
            System.out.printf("start failed, can't connect to local server port: %d\n", port);
            return;
        }

        // new shell
        Shell shell = new Shell(shellProvider, promptCompleter, grpcClient, namespaceManager);

        if (cmd.hasOption(OP_CMD)) {
            String command = cmd.getOptionValue(OP_CMD);

            try {
                String result = shell.execute(command);
                System.out.println(result);
            } catch (Exception e) {
                System.out.printf("illegal command [ %s ], execute failed\n", command);
            }
            return;
        }

        if (cmd.hasOption(OP_FILE)) {
            String filePath = cmd.getOptionValue(OP_FILE);

            String command = "";
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath))));

                command = reader.readLine();
                StringBuilder resultBuilder = new StringBuilder();
                while (null != command) {
                    try {
                        String result = shell.execute(command);
                        resultBuilder.append(result).append("\n");
                    } catch (Exception e) {
                        resultBuilder.append("error\n");
                    }
                    command = reader.readLine();
                }

                System.out.println(resultBuilder);

            } catch (FileNotFoundException e) {
                System.out.printf("error: file %s not found\n", filePath);
            } catch (IOException e) {
                System.out.println("error: io exception");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.printf("illegal command [ %s ], execute failed\n", command);
                e.printStackTrace();
            }

            return;
        }

        shell.start();

        Runtime.getRuntime().addShutdownHook(new Thread(shell::stop));
    }

    public static String getVersion() {
        return ResourceUtil.readStr("VERSION", Charset.defaultCharset());
    }
}
