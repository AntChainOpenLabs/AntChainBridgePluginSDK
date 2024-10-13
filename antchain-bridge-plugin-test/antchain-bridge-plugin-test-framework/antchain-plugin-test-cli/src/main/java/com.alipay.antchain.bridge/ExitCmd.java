package com.alipay.antchain.bridge;

import picocli.CommandLine;

@CommandLine.Command(name = "exit", description = "Exit the application")
class ExitCmd implements Runnable {

    @CommandLine.ParentCommand
    private App parentCommand;

    @Override
    public void run() {
        parentCommand.close();
        System.exit(0);
    }
}