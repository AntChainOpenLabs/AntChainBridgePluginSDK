package org.example;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.Spec;

import java.util.List;
import java.util.Stack;

@Command(name = "help", description = "Displays complete command list")
class HelpCmd implements Runnable {
    @Spec
    CommandSpec spec;

    public void run() {
        printCommandHierarchy(spec.root());
    }

    private void printCommandHierarchy(CommandSpec commandSpec) {
        printCommand(commandSpec, new Stack<>(), 0);
    }
    
    private void printCommand(CommandSpec commandSpec, Stack<String> parentCommands, int indent) {
        parentCommands.push(commandSpec.name());
    
        // Print the current command path and description
        String indentStr = repeat("  ", indent);
        if (indent == 0) {
            // Print main command
            System.out.println("\n" + indentStr + commandSpec.name() + " - " + String.join(" ", commandSpec.usageMessage().description()));
        } else {
            // Print subcommands without including main command name
            System.out.println("\n" + indentStr + String.join(" ", parentCommands.subList(1, parentCommands.size())) + " - " + String.join(" ", commandSpec.usageMessage().description()));
        }
    
        // Print the options and positional parameters
        List<OptionSpec> options = commandSpec.options();
        List<PositionalParamSpec> positionals = commandSpec.positionalParameters();
    
        if (!options.isEmpty() || !positionals.isEmpty()) {
            for (OptionSpec option : options) {
                if (!isHelpOrVersionOption(option)) {
                    System.out.print(indentStr + "  " + String.join(", ", option.names()) + ": " + String.join(" ", option.description()));
                    if (option.required()) {
                        System.out.print(" (required)");
                    }
                    System.out.println();
                }
            }
            for (PositionalParamSpec positional : positionals) {
                System.out.print(indentStr + "  " + positional.paramLabel() + ": " + String.join(" ", positional.description()));
                if (positional.required()) {
                    System.out.print(" (required)");
                }
                System.out.println();
            }
        }
    
        // Recursively print subcommands
        if (!commandSpec.subcommands().isEmpty()) {
            System.out.println(indentStr + "  Subcommands:");
            commandSpec.subcommands().values().forEach(subcommand -> printCommand(subcommand.getCommandSpec(), parentCommands, indent + 1));
        }
    
        parentCommands.pop();
    }
    private boolean isHelpOrVersionOption(OptionSpec option) {
        for (String name : option.names()) {
            if (name.equals("-h") || name.equals("--help") || name.equals("-V") || name.equals("--version")) {
                return true;
            }
        }
        return false;
    }

    private String repeat(String str, int times) {
        if (str == null) {
            return null;
        }
        if (times <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(str.length() * times);
        for (int i = 0; i < times; i++) {
            builder.append(str);
        }
        return builder.toString();
    }
}