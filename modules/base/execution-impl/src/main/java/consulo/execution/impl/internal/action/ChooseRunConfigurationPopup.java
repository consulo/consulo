/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ApplicationPropertiesComponent;
import consulo.component.PropertiesComponent;
import consulo.dataContext.DataContext;
import consulo.execution.*;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.ConfigurationFromContext;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.impl.internal.configuration.RunnerAndConfigurationSettingsImpl;
import consulo.execution.impl.internal.configuration.UnknownConfigurationType;
import consulo.execution.internal.PreferredProducerFind;
import consulo.execution.internal.RunManagerEx;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.runner.RunnerRegistry;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ShortcutProvider;
import consulo.ui.ex.action.ShortcutSet;
import consulo.ui.ex.action.util.ShortcutUtil;
import consulo.ui.ex.popup.*;
import consulo.ui.ex.popup.event.ListPopupKeyListener;
import consulo.ui.image.Image;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import org.jspecify.annotations.Nullable;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.stream.Collectors;

public class ChooseRunConfigurationPopup implements ExecutorProvider {
    private static final KeyCode F4 = KeyCode.of(KeyEvent.VK_F4, "VK_F4");
    private static final KeyCode DELETE = KeyCode.of(KeyEvent.VK_DELETE, "VK_DELETE");
    private static final KeyCode BACK_SPACE = KeyCode.of(KeyEvent.VK_BACK_SPACE, "VK_BACK_SPACE");

    private static final Map<KeyCode, Integer> NUMBER_KEYS = createNumberKeys();

    private final Project myProject;

    private final String myAddKey;

    private final Executor myDefaultExecutor;
    private final @Nullable Executor myAlternativeExecutor;
    private final DataContext myDataContext;

    private @Nullable Executor myCurrentExecutor;
    private boolean myEditConfiguration;
    private @Nullable ListPopup myPopup;

    public ChooseRunConfigurationPopup(
        Project project,
        String addKey,
        Executor defaultExecutor,
        @Nullable Executor alternativeExecutor,
        DataContext dataContext
    ) {
        myProject = project;
        myAddKey = addKey;
        myDefaultExecutor = defaultExecutor;
        myAlternativeExecutor = alternativeExecutor;
        myDataContext = dataContext;
    }

    @RequiredReadAction
    ListPopupStep<?> buildStep() {
        List<ItemWrapper<?>> settingsList = createSettingsList(myProject, this, myDataContext, true);
        return new ConfigurationListPopupStep(this, myProject, myDefaultExecutor.getActionName().get(), settingsList);
    }

    @RequiredUIAccess
    void show(ListPopupStep<?> step) {
        ListPopup popup = JBPopupFactory.getInstance().createListPopup(myProject, step);
        myPopup = popup;
        popup.addKeyListener(new RunListPopupKeyListener());

        String adText = getAdText(myAlternativeExecutor);
        if (adText != null) {
            popup.setAdText(adText);
        }

        popup.showCenteredInCurrentWindow(myProject);
    }

    protected static boolean canRun(Executor executor, RunnerAndConfigurationSettings settings) {
        return ProgramRunnerUtil.getRunner(executor.getId(), settings) != null;
    }

    protected @Nullable String getAdText(@Nullable Executor alternateExecutor) {
        PropertiesComponent properties = ApplicationPropertiesComponent.getInstance();
        if (alternateExecutor != null && !properties.isTrueValue(myAddKey)) {
            return String.format(
                "Hold %s to %s",
                ShortcutUtil.getKeystrokeText(KeyStroke.getKeyStroke("SHIFT")),
                alternateExecutor.getActionName().get()
            );
        }

        if (!properties.isTrueValue("run.configuration.edit.ad")) {
            return String.format("Press %s to Edit", ShortcutUtil.getKeystrokeText(KeyStroke.getKeyStroke("F4")));
        }

        if (!properties.isTrueValue("run.configuration.delete.ad")) {
            return String.format("Press %s to Delete configuration", ShortcutUtil.getKeystrokeText(KeyStroke.getKeyStroke("DELETE")));
        }

        return null;
    }

    private final class RunListPopupKeyListener implements ListPopupKeyListener {
        @Override
        @RequiredUIAccess
        public boolean keyPressed(ListPopup popup, KeyboardInputDetails details) {
            KeyCode keyCode = details.getKeyCode();

            if (KeyCode.SHIFT.equals(keyCode)) {
                myCurrentExecutor = myAlternativeExecutor;
                updatePresentation();
                return true;
            }

            if (KeyCode.ENTER.equals(keyCode) && details.withShift()) {
                popup.handleSelect(true);
                return true;
            }

            if (F4.equals(keyCode)) {
                myEditConfiguration = true;
                popup.handleSelect(true);
                return true;
            }

            if (DELETE.equals(keyCode)) {
                removeSelected(popup);
                return true;
            }

            if (BACK_SPACE.equals(keyCode)) {
                if (isHoldingFilter(popup)) {
                    return false;
                }

                removeSelected(popup);
                return true;
            }

            Integer number = NUMBER_KEYS.get(keyCode);
            if (number != null) {
                return performNumber(popup, number, details.withShift() ? myAlternativeExecutor : myDefaultExecutor);
            }

            return false;
        }

        @Override
        @RequiredUIAccess
        public boolean keyReleased(ListPopup popup, KeyboardInputDetails details) {
            if (KeyCode.SHIFT.equals(details.getKeyCode())) {
                myCurrentExecutor = myDefaultExecutor;
                updatePresentation();
                return true;
            }
            return false;
        }
    }

    private static Map<KeyCode, Integer> createNumberKeys() {
        Map<KeyCode, Integer> keys = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            keys.put(KeyCode.of(KeyEvent.VK_0 + i), i);
            keys.put(KeyCode.of(KeyEvent.VK_NUMPAD0 + i), i);
        }
        return keys;
    }

    private static boolean isHoldingFilter(ListPopup popup) {
        String speedSearchText = popup.getSpeedSearchText();
        return speedSearchText != null && !speedSearchText.isEmpty();
    }

    @RequiredUIAccess
    private boolean performNumber(ListPopup popup, int number, @Nullable Executor executor) {
        if (isHoldingFilter(popup)) {
            return false;
        }

        for (Object item : popup.getListStep().getValues()) {
            if (item instanceof ItemWrapper<?> itemWrapper && itemWrapper.getMnemonic() == number) {
                popup.setFinalRunnable(() -> execute(itemWrapper, executor));
                popup.closeOk(null);
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    private void updatePresentation() {
        ListPopup popup = myPopup;
        if (popup != null) {
            popup.setCaption(getExecutor().getActionName().get());
        }
    }

    private void execute(ItemWrapper<?> itemWrapper, @Nullable Executor executor) {
        if (executor == null) {
            return;
        }

        itemWrapper.perform(myProject, executor, myDataContext);
    }

    void editConfiguration(Project project, RunnerAndConfigurationSettings configuration) {
        Executor executor = getExecutor();
        ApplicationPropertiesComponent.getInstance().setValue("run.configuration.edit.ad", Boolean.toString(true));
        if (RunConfigurationEditor.getInstance(project)
            .editConfiguration(project, configuration, "Edit configuration settings", executor)) {
            RunManagerEx.getInstanceEx(project).setSelectedConfiguration(configuration);
            ExecutionUtil.runConfiguration(configuration, executor);
        }
    }

    private static void deleteConfiguration(Project project, RunnerAndConfigurationSettings configurationSettings) {
        RunManager manager = RunManager.getInstance(project);
        manager.removeConfiguration(configurationSettings);
    }

    @Override
    public Executor getExecutor() {
        return myCurrentExecutor == null ? myDefaultExecutor : myCurrentExecutor;
    }

    private abstract static class Wrapper implements ShortcutProvider {
        private int myMnemonic = -1;
        private final boolean myAddSeparatorAbove;
        private boolean myChecked;

        protected Wrapper(boolean addSeparatorAbove) {
            myAddSeparatorAbove = addSeparatorAbove;
        }

        public int getMnemonic() {
            return myMnemonic;
        }

        public boolean isChecked() {
            return myChecked;
        }

        public void setChecked(boolean checked) {
            myChecked = checked;
        }

        public void setMnemonic(int mnemonic) {
            myMnemonic = mnemonic;
        }

        public boolean addSeparatorAbove() {
            return myAddSeparatorAbove;
        }

        public abstract @Nullable Image getIcon();

        public abstract String getText();

        public boolean canBeDeleted() {
            return false;
        }

        @Override
        public @Nullable ShortcutSet getShortcut() {
            return myMnemonic == -1 ? null : new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_0 + myMnemonic, 0));
        }

        @Override
        public String toString() {
            return "Wrapper[" + getText() + "]";
        }
    }

    private static final class SeparatorWrapper extends Wrapper {
        private SeparatorWrapper() {
            super(false);
        }

        @Override
        public @Nullable Image getIcon() {
            return null;
        }

        @Override
        public String getText() {
            return "";
        }
    }

    public abstract static class ItemWrapper<T> extends Wrapper {
        private final T myValue;
        private boolean myDynamic;

        protected ItemWrapper(@Nullable T value) {
            this(value, false);
        }

        protected ItemWrapper(@Nullable T value, boolean addSeparatorAbove) {
            super(addSeparatorAbove);
            myValue = value;
        }

        public T getValue() {
            return myValue;
        }

        public boolean isDynamic() {
            return myDynamic;
        }

        public void setDynamic(boolean b) {
            myDynamic = b;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return o == this
                || o instanceof ItemWrapper that && Objects.equals(myValue, that.myValue);
        }

        @Override
        public int hashCode() {
            return myValue != null ? myValue.hashCode() : 0;
        }

        public abstract void perform(Project project, Executor executor, DataContext context);

        public @Nullable ConfigurationType getType() {
            return null;
        }

        public boolean available(Executor executor) {
            return false;
        }

        public boolean hasActions() {
            return false;
        }

        public @Nullable PopupStep getNextStep(Project project, ChooseRunConfigurationPopup action) {
            return PopupStep.FINAL_CHOICE;
        }

        public static ItemWrapper<?> wrap(Project project, RunnerAndConfigurationSettings settings, boolean dynamic) {
            ItemWrapper<?> result = wrap(project, settings);
            result.setDynamic(dynamic);
            return result;
        }

        public static ItemWrapper<?> wrap(Project project, RunnerAndConfigurationSettings settings) {
            Image icon = RunManagerEx.getInstanceEx(project).getConfigurationIcon(settings);
            String text = settings.getName();

            return new ItemWrapper<>(settings) {
                @Override
                public void perform(Project project, Executor executor, DataContext context) {
                    RunnerAndConfigurationSettings config = getValue();
                    RunManagerEx.getInstanceEx(project).setSelectedConfiguration(config);
                    ExecutionUtil.runConfiguration(config, executor);
                }

                @Override
                public ConfigurationType getType() {
                    return getValue().getType();
                }

                @Override
                public Image getIcon() {
                    return icon;
                }

                @Override
                public String getText() {
                    return text;
                }

                @Override
                public boolean hasActions() {
                    return true;
                }

                @Override
                public boolean available(Executor executor) {
                    return ProgramRunnerUtil.getRunner(executor.getId(), getValue()) != null;
                }

                @Override
                public PopupStep getNextStep(Project project, ChooseRunConfigurationPopup action) {
                    return new ConfigurationActionsStep(project, action, getValue(), isDynamic());
                }
            };
        }

        @Override
        public boolean canBeDeleted() {
            return !isDynamic() && getValue() instanceof RunnerAndConfigurationSettings;
        }
    }

    private static @Nullable Image getPresentationIcon(Wrapper wrapper, boolean selected) {
        if (wrapper instanceof SeparatorWrapper) {
            return null;
        }

        if (wrapper.isChecked()) {
            return selected ? PlatformIconGroup.actionsChecked_selected() : PlatformIconGroup.actionsChecked();
        }

        Image icon = wrapper.getIcon();
        return icon == null ? Image.empty(Image.DEFAULT_ICON_SIZE) : icon;
    }

    private static List<Wrapper> withSeparators(List<? extends Wrapper> wrappers, boolean groupConfigurations) {
        List<Wrapper> result = new ArrayList<>(wrappers.size());
        for (int i = 0; i < wrappers.size(); i++) {
            if (i > 0 && hasSeparatorAbove(wrappers, i, groupConfigurations)) {
                result.add(new SeparatorWrapper());
            }
            result.add(wrappers.get(i));
        }
        return result;
    }

    private static boolean hasSeparatorAbove(List<? extends Wrapper> wrappers, int index, boolean groupConfigurations) {
        Wrapper value = wrappers.get(index);
        if (value.addSeparatorAbove()) {
            return true;
        }

        if (!groupConfigurations
            || !(value instanceof ItemWrapper<?> configuration)
            || !(wrappers.get(index - 1) instanceof ItemWrapper<?> aboveConfiguration)) {
            return false;
        }

        if (aboveConfiguration.isDynamic() != configuration.isDynamic()) {
            return true;
        }

        ConfigurationType currentType = configuration.getType();
        ConfigurationType aboveType = aboveConfiguration.getType();
        return aboveType != currentType && currentType != null;
    }

    private static final class ConfigurationListPopupStep extends BaseListPopupStep<Wrapper> {
        private final Project myProject;
        private final ChooseRunConfigurationPopup myAction;
        private final MutableFlatDataModel<Wrapper> myModel;
        private final Map<Wrapper, Set<Executor>> myAvailableExecutors = new IdentityHashMap<>();
        private final Map<Wrapper, PopupStep> myNextSteps = new IdentityHashMap<>();
        private final int myDefaultOptionIndex;

        @RequiredReadAction
        private ConfigurationListPopupStep(
            ChooseRunConfigurationPopup action,
            Project project,
            String title,
            List<ItemWrapper<?>> list
        ) {
            this(action, project, title, list, FlatDataModel.of(withSeparators(list, true)));
        }

        @RequiredReadAction
        private ConfigurationListPopupStep(
            ChooseRunConfigurationPopup action,
            Project project,
            String title,
            List<ItemWrapper<?>> list,
            MutableFlatDataModel<Wrapper> model
        ) {
            super(title, model);
            myProject = project;
            myAction = action;
            myModel = model;

            List<Executor> executors = new ArrayList<>(2);
            executors.add(action.myDefaultExecutor);
            if (action.myAlternativeExecutor != null) {
                executors.add(action.myAlternativeExecutor);
            }

            for (ItemWrapper<?> wrapper : list) {
                Set<Executor> available = new HashSet<>();
                for (Executor executor : executors) {
                    if (wrapper.available(executor)) {
                        available.add(executor);
                    }
                }
                myAvailableExecutors.put(wrapper, available);

                if (wrapper.hasActions()) {
                    myNextSteps.put(wrapper, wrapper.getNextStep(project, action));
                }
            }

            RunnerAndConfigurationSettings currentConfiguration = RunManager.getInstance(project).getSelectedConfiguration();
            if (currentConfiguration == null) {
                myDefaultOptionIndex = getDynamicIndex();
            }
            else if (currentConfiguration instanceof RunnerAndConfigurationSettingsImpl) {
                myDefaultOptionIndex = indexOfValue(currentConfiguration);
            }
            else {
                myDefaultOptionIndex = -1;
            }
        }

        private int getDynamicIndex() {
            int i = 0;
            for (Wrapper wrapper : getValues()) {
                if (wrapper instanceof ItemWrapper<?> itemWrapper && itemWrapper.isDynamic()) {
                    return i;
                }
                i++;
            }

            return -1;
        }

        private int indexOfValue(Object value) {
            int i = 0;
            for (Wrapper wrapper : getValues()) {
                if (wrapper instanceof ItemWrapper<?> itemWrapper && value.equals(itemWrapper.getValue())) {
                    return i;
                }
                i++;
            }

            return -1;
        }

        private boolean isAvailable(Wrapper wrapper, Executor executor) {
            Set<Executor> executors = myAvailableExecutors.get(wrapper);
            return executors != null && executors.contains(executor);
        }

        private @Nullable PopupStep getNextStep(Wrapper wrapper) {
            return myNextSteps.get(wrapper);
        }

        @RequiredUIAccess
        private @Nullable Wrapper remove(ItemWrapper<?> removed) {
            List<Wrapper> wrappers = new ArrayList<>();
            int index = -1;
            for (Wrapper wrapper : getValues()) {
                if (wrapper == removed) {
                    index = wrappers.size();
                }
                else if (!(wrapper instanceof SeparatorWrapper)) {
                    wrappers.add(wrapper);
                }
            }

            if (index == -1) {
                return null;
            }

            myModel.replaceAll(withSeparators(wrappers, true));

            if (index < wrappers.size()) {
                return wrappers.get(index);
            }
            return index > 0 ? wrappers.get(index - 1) : null;
        }

        @Override
        public boolean isAutoSelectionEnabled() {
            return false;
        }

        @Override
        public boolean isSeparator(Wrapper value) {
            return value instanceof SeparatorWrapper;
        }

        @Override
        public boolean isSelectable(Wrapper value) {
            return !(value instanceof SeparatorWrapper);
        }

        @Override
        public boolean isSpeedSearchEnabled() {
            return true;
        }

        @Override
        public int getDefaultOptionIndex() {
            return myDefaultOptionIndex;
        }

        @Override
        public boolean hasSubstep(Wrapper selectedValue) {
            return selectedValue instanceof ItemWrapper<?> itemWrapper && itemWrapper.hasActions();
        }

        @Override
        public boolean isFinal(Wrapper wrapper) {
            return myAction.myEditConfiguration
                || isAvailable(wrapper, myAction.getExecutor())
                || getNextStep(wrapper) == FINAL_CHOICE;
        }

        @Override
        public String getTextFor(Wrapper value) {
            return value.getText();
        }

        @Override
        public @Nullable Image getIconFor(Wrapper value) {
            return getPresentationIcon(value, false);
        }

        @Override
        public @Nullable Image getSelectedIconFor(Wrapper value) {
            return getPresentationIcon(value, true);
        }

        @Override
        public @Nullable PopupStep onChosen(Wrapper wrapper, boolean finalChoice) {
            if (!(wrapper instanceof ItemWrapper<?> itemWrapper)) {
                return FINAL_CHOICE;
            }

            if (myAction.myEditConfiguration) {
                Object o = itemWrapper.getValue();
                if (o instanceof RunnerAndConfigurationSettingsImpl runnerAndConfigurationSettings) {
                    return doFinalStep(() -> myAction.editConfiguration(myProject, runnerAndConfigurationSettings));
                }
            }

            Executor executor = myAction.getExecutor();
            if (finalChoice && isAvailable(itemWrapper, executor)) {
                return doFinalStep(() -> {
                    if (executor == myAction.myAlternativeExecutor) {
                        ApplicationPropertiesComponent.getInstance().setValue(myAction.myAddKey, Boolean.toString(true));
                    }

                    itemWrapper.perform(myProject, executor, myAction.myDataContext);
                });
            }
            else {
                return getNextStep(itemWrapper);
            }
        }
    }

    private static final class ConfigurationActionsStep extends BaseListPopupStep<Wrapper> {
        private final RunnerAndConfigurationSettings mySettings;
        private final String myName;
        private final Image myIcon;

        @RequiredReadAction
        private ConfigurationActionsStep(
            Project project,
            ChooseRunConfigurationPopup action,
            RunnerAndConfigurationSettings settings,
            boolean dynamic
        ) {
            super(null, withSeparators(buildActions(project, action, settings, dynamic), false));
            mySettings = settings;
            myName = settings.getName();
            myIcon = RunManagerEx.getInstanceEx(project).getConfigurationIcon(settings);
        }

        public RunnerAndConfigurationSettings getSettings() {
            return mySettings;
        }

        public String getName() {
            return myName;
        }

        public Image getIcon() {
            return myIcon;
        }

        private static List<ActionWrapper> buildActions(
            Project project,
            ChooseRunConfigurationPopup action,
            RunnerAndConfigurationSettings settings,
            boolean dynamic
        ) {
            List<ActionWrapper> result = new ArrayList<>();

            ExecutionTarget active = ExecutionTargetManager.getActiveTarget(project);
            for (ExecutionTarget eachTarget : ExecutionTargetManager.getTargetsToChooseFor(project, settings.getConfiguration())) {
                result.add(new ActionWrapper(eachTarget.getDisplayName(), eachTarget.getIcon()) {
                    {
                        setChecked(eachTarget.equals(active));
                    }

                    @Override
                    public void perform() {
                        RunManagerEx manager = RunManagerEx.getInstanceEx(project);
                        if (dynamic) {
                            manager.setTemporaryConfiguration(settings);
                        }
                        manager.setSelectedConfiguration(settings);

                        ExecutionTargetManager.setActiveTarget(project, eachTarget);
                        ExecutionUtil.runConfiguration(settings, action.getExecutor());
                    }
                });
            }

            boolean isFirst = true;
            for (Executor executor : ExecutorRegistry.getInstance().getRegisteredExecutors()) {
                ProgramRunner runner = RunnerRegistry.getInstance().getRunner(executor.getId(), settings.getConfiguration());
                if (runner != null) {
                    result.add(new ActionWrapper(executor.getActionName().get(), executor.getIcon(), isFirst) {
                        @Override
                        public void perform() {
                            RunManagerEx manager = RunManagerEx.getInstanceEx(project);
                            if (dynamic) {
                                manager.setTemporaryConfiguration(settings);
                            }
                            manager.setSelectedConfiguration(settings);
                            ExecutionUtil.runConfiguration(settings, executor);
                        }
                    });
                    isFirst = false;
                }
            }

            result.add(new ActionWrapper("Edit...", PlatformIconGroup.actionsEditsource(), true) {
                @Override
                public void perform() {
                    RunManagerEx manager = RunManagerEx.getInstanceEx(project);
                    if (dynamic) {
                        manager.setTemporaryConfiguration(settings);
                    }
                    action.editConfiguration(project, settings);
                }
            });

            if (settings.isTemporary() || dynamic) {
                result.add(new ActionWrapper("Save configuration", PlatformIconGroup.actionsMenu_saveall()) {
                    @Override
                    public void perform() {
                        RunManagerEx manager = RunManagerEx.getInstanceEx(project);
                        if (dynamic) {
                            manager.setTemporaryConfiguration(settings);
                        }
                        manager.makeStable(settings);
                    }
                });
            }

            return result;
        }

        @Override
        public boolean isSeparator(Wrapper value) {
            return value instanceof SeparatorWrapper;
        }

        @Override
        public boolean isSelectable(Wrapper value) {
            return !(value instanceof SeparatorWrapper);
        }

        @Override
        public @Nullable PopupStep onChosen(Wrapper selectedValue, boolean finalChoice) {
            return selectedValue instanceof ActionWrapper actionWrapper ? doFinalStep(actionWrapper::perform) : FINAL_CHOICE;
        }

        @Override
        public @Nullable Image getIconFor(Wrapper aValue) {
            return getPresentationIcon(aValue, false);
        }

        @Override
        public @Nullable Image getSelectedIconFor(Wrapper value) {
            return getPresentationIcon(value, true);
        }

        @Override
        public String getTextFor(Wrapper value) {
            return value.getText();
        }
    }

    private abstract static class ActionWrapper extends Wrapper {
        private final String myName;
        private final @Nullable Image myIcon;

        private ActionWrapper(String name, @Nullable Image icon) {
            this(name, icon, false);
        }

        private ActionWrapper(String name, @Nullable Image icon, boolean addSeparatorAbove) {
            super(addSeparatorAbove);
            myName = name;
            myIcon = icon;
        }

        public abstract void perform();

        @Override
        public String getText() {
            return myName;
        }

        @Override
        public @Nullable Image getIcon() {
            return myIcon;
        }
    }

    @RequiredUIAccess
    private void removeSelected(ListPopup popup) {
        PropertiesComponent propertiesComponent = ApplicationPropertiesComponent.getInstance();
        if (!propertiesComponent.isTrueValue("run.configuration.delete.ad")) {
            propertiesComponent.setValue("run.configuration.delete.ad", Boolean.toString(true));
        }

        if (popup.getSelectedValue() instanceof ItemWrapper<?> itemWrapper
            && itemWrapper.canBeDeleted()
            && popup.getListStep() instanceof ConfigurationListPopupStep step) {
            deleteConfiguration(myProject, (RunnerAndConfigurationSettings) itemWrapper.getValue());

            Wrapper next = step.remove(itemWrapper);
            if (next != null) {
                popup.setSelectedValue(next);
            }
        }
    }

    private static class FolderWrapper extends ItemWrapper<String> {
        private final Project myProject;
        private final ExecutorProvider myExecutorProvider;
        private final List<RunnerAndConfigurationSettings> myConfigurations;

        private FolderWrapper(
            Project project,
            ExecutorProvider executorProvider,
            @Nullable String value,
            List<RunnerAndConfigurationSettings> configurations
        ) {
            super(value);
            myProject = project;
            myExecutorProvider = executorProvider;
            myConfigurations = configurations;
        }

        @Override
        public void perform(Project project, Executor executor, DataContext context) {
            RunnerAndConfigurationSettings selectedConfiguration = RunManagerEx.getInstanceEx(project).getSelectedConfiguration();
            if (myConfigurations.contains(selectedConfiguration)) {
                RunManagerEx.getInstanceEx(project).setSelectedConfiguration(selectedConfiguration);
                ExecutionUtil.runConfiguration(selectedConfiguration, myExecutorProvider.getExecutor());
            }
        }

        @Override
        public @Nullable Image getIcon() {
            return PlatformIconGroup.nodesFolder();
        }

        @Override
        public String getText() {
            return getValue();
        }

        @Override
        public boolean hasActions() {
            return true;
        }

        @Override
        public PopupStep getNextStep(Project project, ChooseRunConfigurationPopup action) {
            List<ConfigurationActionsStep> steps = new ArrayList<>();
            for (RunnerAndConfigurationSettings settings : myConfigurations) {
                steps.add(new ConfigurationActionsStep(project, action, settings, false));
            }
            return new FolderStep(myProject, myExecutorProvider, null, steps);
        }
    }

    private static final class FolderStep extends BaseListPopupStep<ConfigurationActionsStep> {
        private final Project myProject;
        private final ExecutorProvider myExecutorProvider;

        private FolderStep(
            Project project,
            ExecutorProvider executorProvider,
            @Nullable String folderName,
            List<ConfigurationActionsStep> children
        ) {
            super(folderName, children, new ArrayList<>());
            myProject = project;
            myExecutorProvider = executorProvider;
        }

        @Override
        public @Nullable PopupStep onChosen(ConfigurationActionsStep selectedValue, boolean finalChoice) {
            if (finalChoice) {
                return doFinalStep(() -> {
                    RunnerAndConfigurationSettings settings = selectedValue.getSettings();
                    RunManagerEx.getInstanceEx(myProject).setSelectedConfiguration(settings);
                    ExecutionUtil.runConfiguration(settings, myExecutorProvider.getExecutor());
                });
            }
            else {
                return selectedValue;
            }
        }

        @Override
        public Image getIconFor(ConfigurationActionsStep aValue) {
            return aValue.getIcon();
        }

        @Override
        public String getTextFor(ConfigurationActionsStep value) {
            return value.getName();
        }

        @Override
        public boolean hasSubstep(ConfigurationActionsStep selectedValue) {
            return !selectedValue.getValues().isEmpty();
        }
    }

    public static List<ItemWrapper> createFlatSettingsList(Project project) {
        return RunManagerImpl.getInstanceImpl(project)
            .getConfigurationsGroupedByTypeAndFolder(false)
            .values()
            .stream()
            .flatMap(map -> map.values().stream().flatMap(Collection::stream))
            .map(settings -> ItemWrapper.wrap(project, settings))
            .collect(Collectors.toList());
    }

    @RequiredReadAction
    public static List<ItemWrapper<?>> createSettingsList(
        Project project,
        ExecutorProvider executorProvider,
        DataContext dataContext,
        boolean createEditAction
    ) {
        List<ItemWrapper<?>> result = new ArrayList<>();

        if (createEditAction) {
            ItemWrapper<Void> edit = new ItemWrapper<>(null) {
                @Override
                public Image getIcon() {
                    return PlatformIconGroup.actionsEditsource();
                }

                @Override
                public String getText() {
                    return ActionLocalize.actionEditrunconfigurationsText().map(Presentation.NO_MNEMONIC).get();
                }

                @Override
                @RequiredUIAccess
                public void perform(Project project, Executor executor, DataContext context) {
                    RunConfigurationEditor.getInstance(project).editAll();
                }

                @Override
                public boolean available(Executor executor) {
                    return true;
                }
            };
            edit.setMnemonic(0);
            result.add(edit);
        }

        RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        RunnerAndConfigurationSettings selectedConfiguration = manager.getSelectedConfiguration();
        if (selectedConfiguration != null) {
            boolean isFirst = true;
            ExecutionTarget activeTarget = ExecutionTargetManager.getActiveTarget(project);
            for (ExecutionTarget eachTarget
                : ExecutionTargetManager.getTargetsToChooseFor(project, selectedConfiguration.getConfiguration())) {
                Image icon = eachTarget.getIcon();
                String text = eachTarget.getDisplayName();
                result.add(new ItemWrapper<>(eachTarget, isFirst) {
                    {
                        setChecked(getValue().equals(activeTarget));
                    }

                    @Override
                    public @Nullable Image getIcon() {
                        return icon;
                    }

                    @Override
                    public String getText() {
                        return text;
                    }

                    @Override
                    public void perform(Project project, Executor executor, DataContext context) {
                        ExecutionTargetManager.setActiveTarget(project, getValue());
                        ExecutionUtil.runConfiguration(selectedConfiguration, executor);
                    }

                    @Override
                    public boolean available(Executor executor) {
                        return true;
                    }
                });
                isFirst = false;
            }
        }

        Map<RunnerAndConfigurationSettings, ItemWrapper<?>> wrappedExisting = new LinkedHashMap<>();
        for (ConfigurationType type : manager.getConfigurationFactories()) {
            if (!(type instanceof UnknownConfigurationType)) {
                Map<String, List<RunnerAndConfigurationSettings>> structure = manager.getStructure(type);
                for (Map.Entry<String, List<RunnerAndConfigurationSettings>> entry : structure.entrySet()) {
                    if (entry.getValue().isEmpty()) {
                        continue;
                    }

                    String key = entry.getKey();
                    if (key != null) {
                        boolean isSelected = entry.getValue().contains(selectedConfiguration);
                        if (isSelected) {
                            assert selectedConfiguration != null;
                        }
                        FolderWrapper folderWrapper = new FolderWrapper(
                            project,
                            executorProvider,
                            key + (isSelected ? "  (mnemonic is to \"" + selectedConfiguration.getName() + "\")" : ""),
                            entry.getValue()
                        );
                        if (isSelected) {
                            folderWrapper.setMnemonic(1);
                        }
                        result.add(folderWrapper);
                    }
                    else {
                        for (RunnerAndConfigurationSettings configuration : entry.getValue()) {
                            ItemWrapper<?> wrapped = ItemWrapper.wrap(project, configuration);
                            if (configuration == selectedConfiguration) {
                                wrapped.setMnemonic(1);
                            }
                            wrappedExisting.put(configuration, wrapped);
                        }
                    }
                }
            }
        }
        if (!DumbService.isDumb(project)) {
            populateWithDynamicRunners(result, wrappedExisting, project, manager, selectedConfiguration, dataContext);
        }
        result.addAll(wrappedExisting.values());
        return result;
    }

    @RequiredReadAction
    private static void populateWithDynamicRunners(
        List<ItemWrapper<?>> result,
        Map<RunnerAndConfigurationSettings, ItemWrapper<?>> existing,
        Project project,
        RunManagerEx manager,
        @Nullable RunnerAndConfigurationSettings selectedConfiguration,
        DataContext dataContext
    ) {
        ConfigurationContext context = ConfigurationContext.getFromContext(dataContext);

        List<ConfigurationFromContext> producers =
            PreferredProducerFind.getConfigurationsFromContext(context.getLocation(), context, false);
        if (producers == null) {
            return;
        }

        Collections.sort(producers, ConfigurationFromContext.NAME_COMPARATOR);

        RunnerAndConfigurationSettings[] preferred = {null};

        int i = 2;
        for (ConfigurationFromContext fromContext : producers) {
            RunnerAndConfigurationSettings configuration = fromContext.getConfigurationSettings();
            if (existing.containsKey(configuration)) {
                ItemWrapper<?> wrapper = existing.get(configuration);
                if (wrapper.getMnemonic() != 1) {
                    wrapper.setMnemonic(i);
                    i++;
                }
            }
            else {
                if (selectedConfiguration != null && configuration.equals(selectedConfiguration)) {
                    continue;
                }

                if (preferred[0] == null) {
                    preferred[0] = configuration;
                }

                Image icon = RunManagerEx.getInstanceEx(project).getConfigurationIcon(configuration);
                String text = configuration.getName();
                ItemWrapper<?> wrapper = new ItemWrapper<>(configuration) {
                    @Override
                    public Image getIcon() {
                        return icon;
                    }

                    @Override
                    public String getText() {
                        return text;
                    }

                    @Override
                    public boolean available(Executor executor) {
                        return canRun(executor, configuration);
                    }

                    @Override
                    public void perform(Project project, Executor executor, DataContext context) {
                        manager.setTemporaryConfiguration(configuration);
                        RunManagerEx.getInstanceEx(project).setSelectedConfiguration(configuration);
                        ExecutionUtil.runConfiguration(configuration, executor);
                    }

                    @Override
                    public PopupStep getNextStep(Project project, ChooseRunConfigurationPopup action) {
                        return new ConfigurationActionsStep(project, action, configuration, isDynamic());
                    }

                    @Override
                    public boolean hasActions() {
                        return true;
                    }
                };

                wrapper.setDynamic(true);
                wrapper.setMnemonic(i);
                result.add(wrapper);
                i++;
            }
        }
    }
}
