/*
 * Copyright 2013-2025 consulo.io
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
package consulo.execution.impl.internal.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AllIcons;
import consulo.application.ReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.dataContext.DataContext;
import consulo.execution.ExecutionManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.ConfigurationFromContext;
import consulo.execution.executor.Executor;
import consulo.execution.impl.internal.configuration.RunnerAndConfigurationSettingsImpl;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.execution.runner.RunnerRegistry;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.CommonDataKeys;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiModificationTracker;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.popup.IPopupChooserBuilder;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2025-01-04
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class RunCurrentFileService {
    private static final Key<RunCurrentFileInfo> CURRENT_FILE_RUN_CONFIGS_KEY = Key.create("CURRENT_FILE_RUN_CONFIGS");

    private static final Logger LOG = Logger.getInstance(RunCurrentFileService.class);

    private record RunCurrentFileInfo(
        long psiModCount,
        @Nonnull List<RunnerAndConfigurationSettings> runConfigs) {
    }

    public void runCurrentFile(@Nonnull Executor executor, @Nonnull AnActionEvent e) {
        Project project = Objects.requireNonNull(e.getData(Project.KEY));
        List<RunnerAndConfigurationSettings> runConfigs = getRunCurrentFileActionStatus(executor, e, true).runConfigs();
        if (runConfigs.isEmpty()) {
            return;
        }

        if (runConfigs.size() == 1) {
            doRunCurrentFile(project, executor, runConfigs.get(0), e.getDataContext());
            return;
        }

        IPopupChooserBuilder<RunnerAndConfigurationSettings> builder = JBPopupFactory.getInstance()
            .createPopupChooserBuilder(runConfigs)
            .setRenderer(new ColoredListCellRenderer<RunnerAndConfigurationSettings>() {
                @Override
                protected void customizeCellRenderer(
                    @Nonnull JList list,
                    RunnerAndConfigurationSettings runConfig,
                    int index,
                    boolean selected,
                    boolean hasFocus
                ) {
                    setIcon(runConfig.getConfiguration().getIcon());
                    append(runConfig.getName());
                }
            })
            .setItemChosenCallback(runConfig -> doRunCurrentFile(project, executor, runConfig, e.getDataContext()));

        InputEvent inputEvent = e.getInputEvent();
        if (inputEvent instanceof MouseEvent) {
            builder.createPopup().showUnderneathOf(inputEvent.getComponent());
        }
        else {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor == null) {
                // Not expected to happen because we are running a file from the current editor.
                LOG.warn("Run Current File (" + runConfigs + "): getSelectedTextEditor() == null");
                return;
            }

            EditorPopupHelper.getInstance().showPopupInBestPositionFor(editor, builder
                .setTitle(executor.getActionName().get())
                .createPopup());
        }
    }

    protected void doRunCurrentFile(
        @Nonnull Project project,
        @Nonnull Executor executor,
        @Nonnull RunnerAndConfigurationSettings runConfig,
        @Nonnull DataContext dataContext
    ) {

        ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.createOrNull(executor, runConfig);
        if (builder == null) {
            return;
        }
        ExecutionManager.getInstance(project).restartRunProfile(builder.activeTarget().dataContext(dataContext).build());
    }

    @Nonnull
    public RunCurrentFileActionStatus getRunCurrentFileActionStatus(
        @Nonnull Executor executor,
        @Nonnull AnActionEvent e,
        boolean resetCache
    ) {
        Project project = Objects.requireNonNull(e.getData(Project.KEY));

        VirtualFile[] files = FileEditorManager.getInstance(project).getSelectedFiles();
        if (files.length == 1) {
            // There's only one visible editor, let's use the file from this editor, even if the editor is not in focus.
            PsiFile psiFile = ReadAction.compute(() -> PsiManager.getInstance(project).findFile(files[0]));
            if (psiFile == null) {
                LocalizeValue tooltip = ExecutionLocalize.runButtonOnToolbarTooltipCurrentFileNotRunnable();
                return RunCurrentFileActionStatus.createDisabled(tooltip, executor.getIcon());
            }

            return getRunCurrentFileActionStatus(executor, psiFile, resetCache, e);
        }

        Editor editor = e.getData(Editor.KEY);
        if (editor == null) {
            LocalizeValue tooltip = ExecutionLocalize.runButtonOnToolbarTooltipCurrentFileNoFocusedEditor();
            return RunCurrentFileActionStatus.createDisabled(tooltip, executor.getIcon());
        }

        PsiFile psiFile = ReadAction.compute(() -> PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()));
        VirtualFile vFile = psiFile != null ? psiFile.getVirtualFile() : null;
        if (psiFile == null || vFile == null || !ArrayUtil.contains(vFile, files)) {
            // This is probably a special editor, like Python Console, which we don't want to use for the 'Run Current File' feature.
            LocalizeValue tooltip = ExecutionLocalize.runButtonOnToolbarTooltipCurrentFileNoFocusedEditor();
            return RunCurrentFileActionStatus.createDisabled(tooltip, executor.getIcon());
        }

        return getRunCurrentFileActionStatus(executor, psiFile, resetCache, e);
    }

    private @Nonnull RunCurrentFileActionStatus getRunCurrentFileActionStatus(
        @Nonnull Executor executor,
        @Nonnull PsiFile psiFile,
        boolean resetCache,
        @Nonnull AnActionEvent e
    ) {
        List<RunnerAndConfigurationSettings> runConfigs = getRunConfigsForCurrentFile(psiFile, resetCache);
        if (runConfigs.isEmpty()) {
            LocalizeValue tooltip = ExecutionLocalize.runButtonOnToolbarTooltipCurrentFileNotRunnable();
            return RunCurrentFileActionStatus.createDisabled(tooltip, executor.getIcon());
        }

        List<RunnerAndConfigurationSettings> runnableConfigs = filterConfigsThatHaveRunner(executor, runConfigs);
        if (runnableConfigs.isEmpty()) {
            return RunCurrentFileActionStatus.createDisabled(
                executor.getStartActiveText(psiFile.getName()),
                executor.getIcon()
            );
        }

        Image icon = executor.getIcon();
        if (runnableConfigs.size() == 1) {
            icon = ExecutorAction.getInformativeIcon(psiFile.getProject(), executor, runnableConfigs.get(0));
        }
        else {
            // myExecutor.getIcon() is the least preferred icon
            // AllIcons.Actions.Restart is more preferred
            // Other icons are the most preferred ones (like ExecutionUtil.getLiveIndicator())
            for (RunnerAndConfigurationSettings config : runnableConfigs) {
                Image anotherIcon = ExecutorAction.getInformativeIcon(psiFile.getProject(), executor, config);
                if (icon == executor.getIcon() || (anotherIcon != executor.getIcon() && anotherIcon != AllIcons.Actions.Restart)) {
                    icon = anotherIcon;
                }
            }
        }

        return RunCurrentFileActionStatus.createEnabled(executor.getStartActiveText(psiFile.getName()), icon,
            runnableConfigs
        );
    }

    @Nonnull
    private List<RunnerAndConfigurationSettings> filterConfigsThatHaveRunner(
        @Nonnull Executor executor,
        @Nonnull List<? extends RunnerAndConfigurationSettings> runConfigs
    ) {
        return ContainerUtil.filter(
            runConfigs,
            config -> RunnerRegistry.getInstance().getRunner(executor.getId(), config.getConfiguration()) != null
        );
    }

    public static List<RunnerAndConfigurationSettings> getRunConfigsForCurrentFile(@Nonnull PsiFile psiFile, boolean resetCache) {
        if (resetCache) {
            psiFile.putUserData(CURRENT_FILE_RUN_CONFIGS_KEY, null);
        }

        // Without this cache, an expensive method `ConfigurationContext.getConfigurationsFromContext()` is called too often for 2 reasons:
        // - there are several buttons on the toolbar (Run, Debug, Profile, etc.), each runs ExecutorAction.update() during each action update session
        // - the state of the buttons on the toolbar is updated several times a second, even if no files are being edited

        // The following few lines do pretty much the same as CachedValuesManager.getCachedValue(), but it's implemented without calling that
        // method because it appeared to be too hard to satisfy both IdempotenceChecker.checkEquivalence() and CachedValueStabilityChecker.checkProvidersEquivalent().
        // The reason is that RunnerAndConfigurationSettings class doesn't implement equals(), and that CachedValueProvider would need to capture
        // ConfigurationContext, which doesn't implement equals() either.
        // Effectively, we need only one boolean value: whether the action is enabled or not, so it shouldn't be a problem that
        // RunnerAndConfigurationSettings and ConfigurationContext don't implement equals() and this code doesn't pass CachedValuesManager checks.

        long psiModCount = PsiModificationTracker.getInstance(psiFile.getProject()).getModificationCount();
        RunCurrentFileInfo cache = psiFile.getUserData(CURRENT_FILE_RUN_CONFIGS_KEY);

        if (cache == null || cache.psiModCount != psiModCount) {
            // The 'Run current file' feature doesn't depend on the caret position in the file, that's why ConfigurationContext is created like this.
            ConfigurationContext configurationContext = new ConfigurationContext(psiFile);

            // The 'Run current file' feature doesn't reuse existing run configurations (by design).
            List<ConfigurationFromContext> configurationsFromContext = configurationContext.createConfigurationsFromContext();

            List<RunnerAndConfigurationSettings> runConfigs = configurationsFromContext == null
                ? List.of()
                : ContainerUtil.map(configurationsFromContext, ConfigurationFromContext::getConfigurationSettings);

            VirtualFile vFile = psiFile.getVirtualFile();
            if (!runConfigs.isEmpty()) {
                String filePath = vFile == null ? null : vFile.getPath();
                for (RunnerAndConfigurationSettings config : runConfigs) {
                    ((RunnerAndConfigurationSettingsImpl) config).setFilePathIfRunningCurrentFile(filePath);
                }
            }

            cache = new RunCurrentFileInfo(psiModCount, runConfigs);
            psiFile.putUserData(CURRENT_FILE_RUN_CONFIGS_KEY, cache);
        }

        return cache.runConfigs;
    }
}
