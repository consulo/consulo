// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.runAnything;

import consulo.dataContext.DataContext;
import consulo.process.cmd.ParametersListUtil;
import jakarta.annotation.Nonnull;

import java.util.*;

public abstract class RunAnythingCommandLineProvider extends RunAnythingNotifiableProvider<String> {

    /**
     * Returns a list of additional aliases for the help command.
     * This default implementation returns an empty list.
     */
    public List<String> getHelpCommandAliases() {
        return Collections.emptyList();
    }

    @Override
    public abstract String getHelpCommand();

    /**
     * Suggests possible variants for completing the command line.
     *
     * @param dataContext the data context
     * @param commandLine the parsed command line
     * @return an iterable of suggested completions
     */
    protected abstract Iterable<String> suggestCompletionVariants(DataContext dataContext, CommandLine commandLine);

    /**
     * Runs the command represented by the parsed command line.
     *
     * @param dataContext the data context
     * @param commandLine the parsed command line
     * @return true if the command executed successfully, false otherwise.
     */
    protected abstract boolean run(DataContext dataContext, CommandLine commandLine);

    @Nonnull
    @Override
    public String getCommand(String value) {
        return value;
    }

    private List<String> getHelpCommands() {
        List<String> result = new ArrayList<>();
        result.add(getHelpCommand());
        result.addAll(getHelpCommandAliases());
        return result;
    }

    @Override
    public String findMatchingValue(DataContext dataContext, String pattern) {
        String[] extracted = extractLeadingHelpPrefix(pattern);
        if (extracted == null) {
            return null;
        }
        String helpCommand = extracted[0];
        if (pattern.startsWith(helpCommand)) {
            return getCommand(pattern);
        }
        return null;
    }

    /**
     * Extracts the leading help prefix from the command line.
     *
     * @param commandLine the full command line
     * @return a two-element array containing the help command and its argument, or null if no help prefix is found.
     */
    private String[] extractLeadingHelpPrefix(String commandLine) {
        List<String> helpCommands = getHelpCommands();
        for (String helpCommand : helpCommands) {
            String prefix = helpCommand + " ";
            if (commandLine.startsWith(prefix)) {
                return new String[]{helpCommand, commandLine.substring(prefix.length())};
            }
            else if (prefix.startsWith(commandLine)) {
                return new String[]{helpCommand, ""};
            }
        }
        return null;
    }

    /**
     * Parses the given command line string into a {@code CommandLine} object.
     *
     * @param commandLine the command line string
     * @return a parsed CommandLine object, or null if the help prefix could not be extracted.
     */
    private CommandLine parseCommandLine(String commandLine) {
        String[] helpPrefix = extractLeadingHelpPrefix(commandLine);
        if (helpPrefix == null) {
            return null;
        }
        String helpCommand = helpPrefix[0];
        String command = helpPrefix[1];

        // Parse the command's parameters.
        List<String> parameters = ParametersListUtil.parse(command, true, true, true);
        String toComplete = parameters.isEmpty() ? "" : parameters.get(parameters.size() - 1);

        String prefix;
        if (command.endsWith(toComplete)) {
            prefix = command.substring(0, command.length() - toComplete.length()).trim();
        }
        else {
            prefix = command.trim();
        }

        List<String> nonEmptyParameters = new ArrayList<>();
        for (String param : parameters) {
            if (!param.isEmpty()) {
                nonEmptyParameters.add(param);
            }
        }

        List<String> completedParameters = new ArrayList<>();
        for (int i = 0; i < parameters.size() - 1; i++) {
            String param = parameters.get(i);
            if (!param.isEmpty()) {
                completedParameters.add(param);
            }
        }
        return new CommandLine(nonEmptyParameters, completedParameters, helpCommand, command.trim(), prefix, toComplete);
    }

    @Override
    public List<String> getValues(DataContext dataContext, String pattern) {
        CommandLine commandLine = parseCommandLine(pattern);
        if (commandLine == null) {
            return Collections.emptyList();
        }
        Iterable<String> variants = suggestCompletionVariants(dataContext, commandLine);
        String helpCommand = commandLine.helpCommand;
        String prefix = commandLine.prefix.isEmpty() ? helpCommand : helpCommand + " " + commandLine.prefix;
        List<String> results = new ArrayList<>();
        for (String variant : variants) {
            results.add(prefix + " " + variant);
        }
        return results;
    }

    @Override
    public boolean run(DataContext dataContext, String value) {
        CommandLine commandLine = parseCommandLine(value);
        if (commandLine == null) {
            return false;
        }
        return run(dataContext, commandLine);
    }

    /**
     * Represents a parsed command line with its parameters and additional data.
     */
    public static class CommandLine {
        public final List<String> parameters;
        public final List<String> completedParameters;
        public final String helpCommand;
        public final String command;
        public final String prefix;
        public final String toComplete;
        private final Set<String> parameterSet;

        public CommandLine(List<String> parameters,
                           List<String> completedParameters,
                           String helpCommand,
                           String command,
                           String prefix,
                           String toComplete) {
            this.parameters = parameters;
            this.completedParameters = completedParameters;
            this.helpCommand = helpCommand;
            this.command = command;
            this.prefix = prefix;
            this.toComplete = toComplete;
            this.parameterSet = new HashSet<>(completedParameters);
        }

        /**
         * Checks whether the specified command exists among the completed parameters.
         *
         * @param command the command string to check
         * @return true if found, false otherwise.
         */
        public boolean contains(String command) {
            return parameterSet.contains(command);
        }
    }
}
