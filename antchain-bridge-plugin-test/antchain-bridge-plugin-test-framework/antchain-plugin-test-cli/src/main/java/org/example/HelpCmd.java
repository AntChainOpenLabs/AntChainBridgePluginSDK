package org.example;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.*;
import picocli.CommandLine.Spec;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

@Command(name = "help", description = "Displays complete command list")
class HelpCmd implements Runnable {
    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        printCommandHierarchy(spec.root());
    }

    private void printCommandHierarchy(CommandSpec commandSpec) {
        printCommand(commandSpec, new ArrayDeque<>(), 0);
    }

    private void printCommand(CommandSpec commandSpec, Deque<String> parentCommands, int indent) {
        parentCommands.addLast(commandSpec.name());

//        String indentStr = "  ".repeat(indent);
        String indentStr = String.join("", Collections.nCopies(indent, "  "));
        String commandPath = String.join(" ", parentCommands);

        // 打印命令路径和描述
        System.out.println("\n" + indentStr + commandPath + " - " + String.join(" ", commandSpec.usageMessage().description()));

        // 打印选项和位置参数
        printOptionsAndPositionals(commandSpec, indent + 1);

        // 递归打印子命令
        if (!commandSpec.subcommands().isEmpty()) {
            System.out.println(indentStr + "  Subcommands:");
            for (CommandLine subcommandLine : commandSpec.subcommands().values()) {
                CommandSpec subcommandSpec = subcommandLine.getCommandSpec();
                printCommand(subcommandSpec, parentCommands, indent + 1);
            }
        }

        parentCommands.removeLast();
    }

    private void printOptionsAndPositionals(CommandSpec commandSpec, int indent) {
        List<OptionSpec> options = commandSpec.options();
        List<PositionalParamSpec> positionals = commandSpec.positionalParameters();

        if (options.isEmpty() && positionals.isEmpty()) {
            return;
        }

//        String indentStr = "  ".repeat(indent);
        String indentStr = String.join("", Collections.nCopies(indent, "  "));
        int maxOptionLength = 0;

        // 计算选项名称的最大长度以对齐描述
        for (OptionSpec option : options) {
            if (!isHelpOrVersionOption(option)) {
                String optionNames = String.join(", ", option.names());
                maxOptionLength = Math.max(maxOptionLength, optionNames.length());
            }
        }
        for (PositionalParamSpec positional : positionals) {
            String paramLabel = positional.paramLabel();
            maxOptionLength = Math.max(maxOptionLength, paramLabel.length());
        }

        // 格式化并打印选项
        for (OptionSpec option : options) {
            if (!isHelpOrVersionOption(option)) {
                String optionNames = String.join(", ", option.names());
                String description = String.join(" ", option.description());
                String required = option.required() ? " (required)" : "";
                System.out.printf("%s  %-" + maxOptionLength + "s  %s%s%n", indentStr, optionNames, description, required);
            }
        }

        // 格式化并打印位置参数
        for (PositionalParamSpec positional : positionals) {
            String paramLabel = positional.paramLabel();
            String description = String.join(" ", positional.description());
            String required = positional.required() ? " (required)" : "";
            System.out.printf("%s  %-" + maxOptionLength + "s  %s%s%n", indentStr, paramLabel, description, required);
        }
    }

    private boolean isHelpOrVersionOption(OptionSpec option) {
        for (String name : option.names()) {
            if (name.equals("-h") || name.equals("--help") || name.equals("-V") || name.equals("--version")) {
                return true;
            }
        }
        return false;
    }
}