/*
 * Copyright 2000-2026 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.execution.impl.internal.mcp;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.execution.CommonProgramRunConfigurationParameters;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.ConfigurationFromContext;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.lineMarker.RunLineMarkerContributor;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.mcp.tool.McpParameter;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcpServer.McpProjectPaths;
import consulo.mcpServer.McpToolActions;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolGroup;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
@Singleton
public class ExecutionToolset implements McpToolset {
    private static final int DEFAULT_TIMEOUT_MILLIS = 120_000;
    private static final int MAX_ELEMENT_TEXT = 80;

    @Override
    public McpToolGroup getGroup() {
        return McpToolGroup.BUILD_AND_RUN;
    }

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("get_run_configurations")
            .description("Without filePath, lists the run configurations of the project: name, type and, when the configuration has "
                + "them, program arguments and working directory; 'overrides' marks configurations that accept the launch overrides of "
                + "execute_run_configuration. With filePath, lists the run points of that file instead - lines with a Run gutter icon, "
                + "such as main methods or tests - to pass as filePath + line to execute_run_configuration.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .param(McpParameter.string("filePath", "Optional project-relative file whose run points to list.").optional())
            .handler(ExecutionToolset::getRunConfigurations);

        registrar.tool("execute_run_configuration")
            .description("Runs an existing run configuration by name, or a temporary one created from a run point (filePath + line from "
                + "get_run_configurations), in the Run tool window. With waitForExit (default) it waits up to timeout milliseconds and "
                + "returns the exit code and the output; when the timeout expires the process keeps running and the result says so. "
                + "Long output is cut and the path of a file with the full output is returned. programArguments, workingDirectory and "
                + "envs override the configuration for this launch only; an empty string keeps the configured value, a single space "
                + "clears it.")
            .openWorld()
            .requiresProject()
            .param(McpParameter.string("configurationName", "Name of an existing run configuration.").optional())
            .param(McpParameter.string("filePath", "Project-relative file of a run point; use together with line.").optional())
            .param(McpParameter.integer("line", "1-based line of the run point in filePath.").optional())
            .param(McpParameter.integer("timeout", "Milliseconds to wait for the process to exit.").defaultValue(DEFAULT_TIMEOUT_MILLIS))
            .param(McpParameter.bool("waitForExit", "Whether to wait for the process to exit.").defaultValue(true))
            .param(McpParameter.string("programArguments", "Program arguments for this launch only.").optional())
            .param(McpParameter.string("workingDirectory", "Working directory for this launch only.").optional())
            .param(McpParameter.stringArray("envs", "Environment variables for this launch only, as KEY=VALUE, merged over the configured ones.")
                .optional())
            .handler(ExecutionToolset::executeRunConfiguration);
    }

    private static CompletableFuture<McpToolCallResult> getRunConfigurations(McpToolCallContext context) {
        Project project = context.getProject();
        String filePath = context.findString("filePath");
        if (filePath != null && !filePath.isBlank()) {
            return McpToolActions.uiActionAsync(project, () -> {
                PsiDocumentManager.getInstance(project).commitAllDocuments();
                if (DumbService.isDumb(project)) {
                    return CompletableFuture.completedFuture(McpToolCallResult.error("The IDE is building its indexes, retry when indexing has finished."));
                }
                return McpToolActions.readAction(project, () -> describeRunPoints(project, filePath));
            });
        }

        return McpToolActions.readAction(project, () -> {
            StringBuilder builder = new StringBuilder();
            for (RunnerAndConfigurationSettings settings : RunManager.getInstance(project).getAllSettings()) {
                builder.append(settings.getName()).append(" [").append(settings.getType().getDisplayName().get()).append(']');
                if (settings.getConfiguration() instanceof CommonProgramRunConfigurationParameters parameters) {
                    builder.append(" overrides");
                    if (!StringUtil.isEmptyOrSpaces(parameters.getProgramParameters())) {
                        builder.append(" args: ").append(parameters.getProgramParameters());
                    }
                    if (!StringUtil.isEmptyOrSpaces(parameters.getWorkingDirectory())) {
                        builder.append(" workingDirectory: ").append(parameters.getWorkingDirectory());
                    }
                }
                builder.append('\n');
            }
            return McpToolCallResult.text(builder.isEmpty() ? "No run configurations." : builder.toString());
        });
    }

    private static McpToolCallResult describeRunPoints(Project project, String filePath) {
        VirtualFile file = McpProjectPaths.resolve(project, filePath);
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (psiFile == null || document == null) {
            return McpToolCallResult.error("File is not backed by a PSI tree: " + filePath);
        }

        Map<Integer, String> runPoints = new TreeMap<>();
        for (PsiElement leaf = PsiTreeUtil.firstChild(psiFile); leaf != null; leaf = PsiTreeUtil.nextLeaf(leaf)) {
            RunLineMarkerContributor.Info info = findInfo(leaf);
            if (info == null) {
                continue;
            }
            int line = document.getLineNumber(leaf.getTextOffset()) + 1;
            if (runPoints.containsKey(line)) {
                continue;
            }
            ConfigurationFromContext configuration = findConfigurationFromContext(leaf);
            if (configuration == null) {
                continue;
            }
            String tooltip = info.tooltipProvider == null ? null : info.tooltipProvider.apply(leaf);
            String text = StringUtil.first(configuration.getSourceElement().getText().trim().replace('\n', ' '), MAX_ELEMENT_TEXT, true);
            runPoints.put(line, "line " + line + ": " + configuration.getConfigurationSettings().getName()
                + (StringUtil.isEmptyOrSpaces(tooltip) ? "" : " (" + StringUtil.stripHtml(tooltip, false).trim() + ")")
                + " - " + text);
        }

        if (runPoints.isEmpty()) {
            return McpToolCallResult.text("No run points in " + filePath);
        }
        return McpToolCallResult.text(String.join("\n", runPoints.values()) + "\n");
    }

    private static RunLineMarkerContributor.@Nullable Info findInfo(PsiElement element) {
        for (RunLineMarkerContributor contributor : RunLineMarkerContributor.forLanguage(element.getLanguage())) {
            RunLineMarkerContributor.Info info = contributor.getInfo(element);
            if (info != null) {
                return info;
            }
        }
        return null;
    }

    private static @Nullable ConfigurationFromContext findConfigurationFromContext(PsiElement start) {
        for (PsiElement element = start; element != null && !(element instanceof PsiFile); element = element.getParent()) {
            List<ConfigurationFromContext> configurations = new ConfigurationContext(element).getConfigurationsFromContext();
            if (configurations != null && !configurations.isEmpty()) {
                return configurations.get(0);
            }
        }
        return null;
    }

    private static CompletableFuture<McpToolCallResult> executeRunConfiguration(McpToolCallContext context) {
        Project project = context.getProject();
        String configurationName = context.findString("configurationName");
        String filePath = context.findString("filePath");
        String lineValue = context.findString("line");
        int timeout = context.getInt("timeout");
        boolean waitForExit = context.getBoolean("waitForExit");
        String programArguments = context.findString("programArguments");
        String workingDirectory = context.findString("workingDirectory");
        List<String> envs = context.getStringList("envs");

        boolean byName = configurationName != null && !configurationName.isBlank();
        boolean byLocation = filePath != null && !filePath.isBlank();
        if (byName == byLocation) {
            return CompletableFuture.completedFuture(McpToolCallResult.error("Pass either configurationName, or filePath together with line."));
        }

        return McpToolActions.uiActionAsync(project, () -> {
            RunnerAndConfigurationSettings settings;
            if (byName) {
                settings = RunManager.getInstance(project).findConfigurationByName(configurationName);
                if (settings == null) {
                    return CompletableFuture.completedFuture(McpToolCallResult.error("No such run configuration: " + configurationName));
                }
            }
            else {
                PsiDocumentManager.getInstance(project).commitAllDocuments();
                settings = fromCodeLocation(project, filePath, lineValue);
                if (settings == null) {
                    return CompletableFuture.completedFuture(McpToolCallResult.error("No run configuration can be created from " + filePath
                        + ":" + lineValue + ". Use get_run_configurations with filePath to find run points."));
                }
            }

            RunnerAndConfigurationSettings launched;
            try {
                launched = applyOverrides(project, settings, programArguments, workingDirectory, envs);
            }
            catch (IllegalArgumentException e) {
                return CompletableFuture.completedFuture(McpToolCallResult.error(String.valueOf(e.getMessage())));
            }
            return McpRunConfigurationLauncher.launch(project, launched, DefaultRunExecutor.getRunExecutorInstance(), timeout, waitForExit);
        });
    }

    private static @Nullable RunnerAndConfigurationSettings fromCodeLocation(Project project, String filePath, @Nullable String lineValue) {
        int line;
        try {
            line = lineValue == null ? -1 : Integer.parseInt(lineValue.trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
        VirtualFile file = McpProjectPaths.resolve(project, filePath);
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (psiFile == null || document == null || line < 1 || line > document.getLineCount()) {
            return null;
        }

        int start = document.getLineStartOffset(line - 1);
        String lineText = document.getText().substring(start, document.getLineEndOffset(line - 1));
        int indent = 0;
        while (indent < lineText.length() && Character.isWhitespace(lineText.charAt(indent))) {
            indent++;
        }
        PsiElement element = psiFile.findElementAt(start + indent);
        ConfigurationFromContext configuration = element == null ? null : findConfigurationFromContext(element);
        if (configuration == null) {
            return null;
        }

        RunnerAndConfigurationSettings settings = configuration.getConfigurationSettings();
        settings.setTemporary(true);
        RunManager.getInstance(project).addConfiguration(settings);
        return settings;
    }

    private static RunnerAndConfigurationSettings applyOverrides(Project project,
                                                                 RunnerAndConfigurationSettings settings,
                                                                 @Nullable String programArguments,
                                                                 @Nullable String workingDirectory,
                                                                 List<String> envs) {
        boolean hasArguments = programArguments != null && !programArguments.isEmpty();
        boolean hasWorkingDirectory = workingDirectory != null && !workingDirectory.isEmpty();
        if (!hasArguments && !hasWorkingDirectory && envs.isEmpty()) {
            return settings;
        }

        RunConfiguration copy = settings.getConfiguration().clone();
        if (!(copy instanceof CommonProgramRunConfigurationParameters parameters)) {
            throw new IllegalArgumentException("Run configuration '" + settings.getName() + "' does not support programArguments, "
                + "workingDirectory or envs overrides.");
        }
        if (hasArguments) {
            parameters.setProgramParameters(programArguments.isBlank() ? null : programArguments);
        }
        if (hasWorkingDirectory) {
            parameters.setWorkingDirectory(workingDirectory.isBlank() ? null : workingDirectory);
        }
        if (!envs.isEmpty()) {
            Map<String, String> merged = new LinkedHashMap<>(parameters.getEnvs());
            for (String entry : envs) {
                int separator = entry.indexOf('=');
                if (separator <= 0) {
                    throw new IllegalArgumentException("Environment entries must be KEY=VALUE: " + entry);
                }
                merged.put(entry.substring(0, separator), entry.substring(separator + 1));
            }
            parameters.setEnvs(merged);
        }
        ConfigurationFactory factory = settings.getFactory();
        if (factory == null) {
            throw new IllegalArgumentException("Run configuration '" + settings.getName() + "' has no factory to apply launch overrides.");
        }
        return RunManager.getInstance(project).createConfiguration(copy, factory);
    }
}
