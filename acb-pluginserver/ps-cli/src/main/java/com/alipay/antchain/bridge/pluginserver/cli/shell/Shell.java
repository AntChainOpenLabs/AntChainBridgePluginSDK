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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.alipay.antchain.bridge.pluginserver.cli.command.NamespaceManager;
import com.alipay.antchain.bridge.pluginserver.cli.core.ManagementGrpcClient;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class Shell {

    private static final String PROMPT = "\033[0;37mps> \033[0m";

    private final NamespaceManager namespaceManager;

    private Terminal terminal;

    private GlLineReader reader;

    private final Map<String, ReservedWord> reservedWord = new HashMap<>();

    private AtomicBoolean loopRunning = new AtomicBoolean(false);

    private final ReentrantLock shellLock = new ReentrantLock();

    private final PromptCompleter completer;

    private final ShellProvider shellProvider;

    public final static Runtime RUNTIME = new Runtime();

    public Shell(ShellProvider shellProvider, PromptCompleter completer, ManagementGrpcClient grpcClient,
                 NamespaceManager namespaceManager) {

        // 不可扩展参数初始化
        init();

        // 扩展初始化
        this.shellProvider = shellProvider;
        this.namespaceManager = namespaceManager;

        this.completer = completer;
        this.reservedWord.keySet().forEach(reservedWord -> this.completer.addReservedWord(reservedWord));

        reader.setCompleter(completer);

        // 运行时设置
        RUNTIME.setGrpcClient(grpcClient);
    }

    void init() {
        // init term
        try {
            terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("can't open system stream");
        }

        // set printer
        RUNTIME.setPrinter(terminal.writer());

        // init linereader
        reader = new GlLineReader(terminal, "mychain-gl", new HashMap<>());

        reader.setVariable(LineReader.HISTORY_FILE, Paths.get("./clihistory.tmp"));
        reader.setHistory(new DefaultHistory(reader));

        reader.unsetOpt(Option.MENU_COMPLETE);
        reader.setOpt(Option.AUTO_LIST);
        reader.unsetOpt(Option.AUTO_MENU);


        // init shell commands
        initReservedWord();
    }

    public void start() {

        try {
            if (shellLock.tryLock()) {

                if (loopRunning.get()) {
                    return;
                }

                loopRunning.set(true);

                welcome();

                new Thread(() -> {
                    // start loop
                    while (loopRunning.get()) {
                        String cmd = reader.readLine(PROMPT);

                        if (null == cmd || cmd.isEmpty()) {
                            continue;
                        }

                        try {
                            if (reservedWord.containsKey(cmd.trim())) {

                                reservedWord.get(cmd.trim()).execute();
                                continue;
                            }

                            String result = this.shellProvider.execute(cmd);
                            if (null != result) {
                                if (result.startsWith("{") || result.startsWith("[")) {
                                    RUNTIME.getPrinter().println(JsonUtil.format(result));
                                } else {
                                    RUNTIME.getPrinter().println(result);
                                }

                            }
                        } catch (Exception e) {
                            RUNTIME.getPrinter().println("shell evaluate fail:" + e.getMessage());
                        }
                    }
                }, "shell_thread").start();

            }
        } finally {
            shellLock.unlock();
        }
    }

    public String execute(String cmd) {
        return this.shellProvider.execute(cmd);
    }

    public void stop() {

        loopRunning.set(false);
        try {
            if (null != RUNTIME.getGrpcClient()) {
                RUNTIME.getGrpcClient().shutdown();
            }
        } catch (InterruptedException e) {
            // not process
        }
    }

    protected void initReservedWord() {
        this.reservedWord.put("exit", this::exit);
        this.reservedWord.put("help", this::help);
    }

    protected void exit() {
        stop();
    }

    protected void help() {
        RUNTIME.getPrinter().print(namespaceManager.dump());
    }

    protected void welcome() {
        RUNTIME.printer.println(
                "    ___            __   ______ __            _           ____         _      __\n" +
                "   /   |   ____   / /_ / ____// /_   ____ _ (_)____     / __ ) _____ (_)____/ /____ _ ___\n" +
                "  / /| |  / __ \\ / __// /    / __ \\ / __ `// // __ \\   / __  |/ ___// // __  // __ `// _ \\\n" +
                " / ___ | / / / // /_ / /___ / / / // /_/ // // / / /  / /_/ // /   / // /_/ // /_/ //  __/\n" +
                "/_/  |_|/_/ /_/ \\__/ \\____//_/ /_/ \\__,_//_//_/ /_/  /_____//_/   /_/ \\__,_/ \\__, / \\___/\n" +
                "                                                                            /____/        \n" +
                "                          PLUGIN SERVER CLI " + Launcher.getVersion()
        );
        RUNTIME.printer.println("\n>>> type help to see all commands...");
    }

    public static class Runtime {

        private PrintWriter printer;

        private ManagementGrpcClient grpcClient;

        void setPrinter(PrintWriter printer) {

            this.printer = printer;
        }

        void setGrpcClient(ManagementGrpcClient grpcClient) {
            this.grpcClient = grpcClient;
        }

        public PrintWriter getPrinter() {
            return printer;
        }

        public ManagementGrpcClient getGrpcClient() {
            return grpcClient;
        }

    }
}
