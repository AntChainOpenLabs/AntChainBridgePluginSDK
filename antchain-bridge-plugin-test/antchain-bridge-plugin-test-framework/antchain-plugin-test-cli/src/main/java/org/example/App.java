package org.example;

import java.io.IOException;
import java.util.Scanner;

import org.example.plugintestrunner.exception.TestCaseException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import org.example.plugintestrunner.PluginTestRunner;

@Command(name = "", mixinStandardHelpOptions = true, description = "CLI tool for plugin testing", subcommands = {
    PluginTestCmd.class,
    PluginManagerCmd.class,
    ChainManagerCmd.class,
    HelpCmd.class})
public class App implements Runnable {

    public PluginTestRunner pluginTestRunner;

    @Override
    public void run() {}

    public void init() throws IOException, TestCaseException {
        pluginTestRunner = PluginTestRunner.init();
    }

    public static void main(String[] args) throws IOException, TestCaseException {
        App app = new App();
        app.init();
        CommandLine commandLine = new CommandLine(app);

        printHelloInfo();
        runCommandLoop(commandLine);
    }

    private static void runCommandLoop(CommandLine commandLine) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("cmd> ");
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input)) {
                    break;
                }
                commandLine.execute(input.split("\\s+"));
            }
        }
    }

    private static void printHelloInfo() {
        System.out.println("  ____  _             _         _____         _  _____           _ ");
        System.out.println(" |  _ \\| |_   _  __ _(_)_ __   |_   _|__  ___| ||_   _|__   ___ | |");
        System.out.println(" | |_) | | | | |/ _` | | '_ \\    | |/ _ \\/ __| __|| |/ _ \\ / _ \\| |");
        System.out.println(" |  __/| | |_| | (_| | | | | |   | |  __/\\__ \\ |_ | | (_) | (_) | |");
        System.out.println(" |_|   |_|\\__,_|\\__, |_|_| |_|   |_|\\___||___/\\__||_|\\___/ \\___/|_|");
        System.out.println("                |___/                                               ");
        System.out.println("                        Plugin TestTool CLI 0.1.0");
        System.out.println(">>> type help to see all commands...");
    }
}