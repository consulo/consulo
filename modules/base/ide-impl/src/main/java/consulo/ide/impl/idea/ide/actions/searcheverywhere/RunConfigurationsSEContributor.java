// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.application.progress.ProgressIndicator;
import consulo.application.util.function.Processor;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.NameUtil;
import consulo.dataContext.DataManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.runner.RunnerRegistry;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.impl.internal.action.ChooseRunConfigurationPopup;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.MagicConstant;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class RunConfigurationsSEContributor implements SearchEverywhereContributor<ChooseRunConfigurationPopup.ItemWrapper> {
    private final SearchEverywhereCommandInfo RUN_COMMAND = new SearchEverywhereCommandInfo(
        "run",
        IdeLocalize.searcheverywhereRunconfigurationsCommandRunDescription().get(),
        this
    );
    private final SearchEverywhereCommandInfo DEBUG_COMMAND = new SearchEverywhereCommandInfo(
        "debug",
        IdeLocalize.searcheverywhereRunconfigurationsCommandDebugDescription().get(),
        this
    );

    private final static int RUN_MODE = 0;
    private final static int DEBUG_MODE = 1;

    private final Project myProject;
    private final Component myContextComponent;
    private final Supplier<String> myCommandSupplier;

    public RunConfigurationsSEContributor(Project project, Component component, Supplier<String> commandSupplier) {
        myProject = project;
        myContextComponent = component;
        myCommandSupplier = commandSupplier;
    }

    @Nonnull
    @Override
    public String getSearchProviderId() {
        return getClass().getSimpleName();
    }

    @Nonnull
    @Override
    public String getGroupName() {
        return IdeLocalize.searcheverywhereRunConfigsTabName().get();
    }

    @Override
    public int getSortWeight() {
        return 350;
    }

    @Override
    public boolean showInFindResults() {
        return false;
    }

    @Override
    public boolean processSelectedItem(
        @Nonnull ChooseRunConfigurationPopup.ItemWrapper selected,
        int modifiers,
        @Nonnull String searchText
    ) {
        RunnerAndConfigurationSettings settings = ObjectUtil.tryCast(selected.getValue(), RunnerAndConfigurationSettings.class);
        if (settings != null) {
            int mode = getMode(searchText, modifiers);
            Executor executor = findExecutor(settings, mode);
            if (executor != null) {
                DataManager dataManager = DataManager.getInstance();
                selected.perform(myProject, executor, dataManager.getDataContext(myContextComponent));
            }
        }

        return true;
    }

    @Nullable
    @Override
    public Object getDataForItem(@Nonnull ChooseRunConfigurationPopup.ItemWrapper element, @Nonnull Key dataId) {
        return null;
    }

    @Nonnull
    @Override
    public ListCellRenderer<? super ChooseRunConfigurationPopup.ItemWrapper> getElementsRenderer() {
        return renderer;
    }

    @Nonnull
    @Override
    public List<SearchEverywhereCommandInfo> getSupportedCommands() {
        return Arrays.asList(RUN_COMMAND, DEBUG_COMMAND);
    }

    @Override
    public void fetchElements(
        @Nonnull String pattern,
        @Nonnull ProgressIndicator progressIndicator,
        @Nonnull Processor<? super ChooseRunConfigurationPopup.ItemWrapper> consumer
    ) {

        if (StringUtil.isEmptyOrSpaces(pattern)) {
            return;
        }

        pattern = filterString(pattern);
        MinusculeMatcher matcher = NameUtil.buildMatcher(pattern).build();
        for (ChooseRunConfigurationPopup.ItemWrapper wrapper : ChooseRunConfigurationPopup.createFlatSettingsList(myProject)) {
            if (matcher.matches(wrapper.getText()) && !consumer.process(wrapper)) {
                return;
            }
        }
    }

    @MagicConstant(intValues = {RUN_MODE, DEBUG_MODE})
    private int getMode(String searchText, int modifiers) {
        if (isCommand(searchText, DEBUG_COMMAND)) {
            return DEBUG_MODE;
        }
        else if (isCommand(searchText, RUN_COMMAND)) {
            return RUN_MODE;
        }
        else {
            return (modifiers & InputEvent.SHIFT_MASK) == 0 ? DEBUG_MODE : RUN_MODE;
        }
    }

    private static Optional<String> extractFirstWord(String input) {
        if (!StringUtil.isEmptyOrSpaces(input) && input.contains(" ")) {
            return Optional.of(input.split(" ")[0]);
        }

        return Optional.empty();
    }

    private String filterString(String input) {
        return extractFirstWord(input).filter(firstWord -> RUN_COMMAND.getCommandWithPrefix()
                .startsWith(firstWord) || DEBUG_COMMAND.getCommandWithPrefix().startsWith(firstWord))
            .map(firstWord -> input.substring(firstWord.length() + 1)).orElse(input);
    }

    private static boolean isCommand(String input, SearchEverywhereCommandInfo command) {
        if (input == null) {
            return false;
        }

        return extractFirstWord(input).map(firstWord -> command.getCommandWithPrefix().startsWith(firstWord)).orElse(false);
    }

    private final Renderer renderer = new Renderer();

    private class Renderer extends JPanel implements ListCellRenderer<ChooseRunConfigurationPopup.ItemWrapper> {

        private final SimpleColoredComponent runConfigInfo = new SimpleColoredComponent();
        private final SimpleColoredComponent executorInfo = new SimpleColoredComponent();

        private Renderer() {
            super(new BorderLayout());
            add(runConfigInfo, BorderLayout.CENTER);
            add(executorInfo, BorderLayout.EAST);
            setBorder(JBUI.Borders.empty(1, /*UIUtil.isUnderWin10LookAndFeel() ? 0 : */JBUIScale.scale(UIUtil.getListCellHPadding())));
        }

        @Override
        public Component getListCellRendererComponent(
            JList<? extends ChooseRunConfigurationPopup.ItemWrapper> list,
            ChooseRunConfigurationPopup.ItemWrapper wrapper,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            runConfigInfo.clear();
            executorInfo.clear();

            setBackground(UIUtil.getListBackground(isSelected, true));
            setFont(list.getFont());
            Color foreground = isSelected ? list.getSelectionForeground() : list.getForeground();
            runConfigInfo.append(wrapper.getText(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, foreground));
            runConfigInfo.setIcon(wrapper.getIcon());

            fillExecutorInfo(wrapper, list, isSelected);

            return this;
        }

        private void fillExecutorInfo(ChooseRunConfigurationPopup.ItemWrapper wrapper, JList<?> list, boolean selected) {

            SimpleTextAttributes commandAttributes = selected ? new SimpleTextAttributes(
                SimpleTextAttributes.STYLE_PLAIN,
                list.getSelectionForeground()
            ) : SimpleTextAttributes.GRAYED_ATTRIBUTES;
            SimpleTextAttributes shortcutAttributes = selected ? new SimpleTextAttributes(
                SimpleTextAttributes.STYLE_PLAIN,
                list.getSelectionForeground()
            ) : SimpleTextAttributes.GRAY_ATTRIBUTES;

            String input = myCommandSupplier.get();
            if (isCommand(input, RUN_COMMAND)) {
                fillWithMode(wrapper, RUN_MODE, commandAttributes);
                return;
            }
            if (isCommand(input, DEBUG_COMMAND)) {
                fillWithMode(wrapper, DEBUG_MODE, commandAttributes);
                return;
            }

            Executor debugExecutor = ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.DEBUG);
            Executor runExecutor = DefaultRunExecutor.getRunExecutorInstance();

            KeyStroke enterStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
            KeyStroke shiftEnterStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK);
            if (debugExecutor != null) {
                executorInfo.append(debugExecutor.getId(), commandAttributes);
                executorInfo.append("(" + KeymapUtil.getKeystrokeText(enterStroke) + ")", shortcutAttributes);
                if (runExecutor != null) {
                    executorInfo.append(" / " + runExecutor.getId(), commandAttributes);
                    executorInfo.append("(" + KeymapUtil.getKeystrokeText(shiftEnterStroke) + ")", shortcutAttributes);
                }
            }
            else if (runExecutor != null) {
                executorInfo.append(runExecutor.getId(), commandAttributes);
                executorInfo.append("(" + KeymapUtil.getKeystrokeText(enterStroke) + ")", shortcutAttributes);
            }
        }

        private void fillWithMode(
            ChooseRunConfigurationPopup.ItemWrapper wrapper,
            @MagicConstant(intValues = {RUN_MODE, DEBUG_MODE}) int mode,
            SimpleTextAttributes attributes
        ) {
            Optional.ofNullable(ObjectUtil.tryCast(wrapper.getValue(), RunnerAndConfigurationSettings.class))
                .map(settings -> findExecutor(settings, mode))
                .ifPresent(executor -> {
                    executorInfo.append(executor.getId(), attributes);
                    executorInfo.setIcon(executor.getToolWindowIcon());
                });
        }
    }

    @Nullable
    private static Executor findExecutor(
        @Nonnull RunnerAndConfigurationSettings settings,
        @MagicConstant(intValues = {RUN_MODE, DEBUG_MODE}) int mode
    ) {
        Executor runExecutor = DefaultRunExecutor.getRunExecutorInstance();
        Executor debugExecutor = ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.DEBUG);

        Executor executor = mode == RUN_MODE ? runExecutor : debugExecutor;
        if (executor == null) {
            return null;
        }

        RunConfiguration runConf = settings.getConfiguration();
        if (RunnerRegistry.getInstance().getRunner(executor.getId(), runConf) == null) {
            executor = runExecutor == executor ? debugExecutor : runExecutor;
        }

        return executor;
    }
}
