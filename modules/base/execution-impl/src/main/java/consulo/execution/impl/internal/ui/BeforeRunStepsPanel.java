/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.execution.impl.internal.ui;

import consulo.application.ApplicationPropertiesComponent;
import consulo.execution.BeforeRunTask;
import consulo.execution.BeforeRunTaskProvider;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.impl.internal.RunConfigurationBeforeRunProvider;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.impl.internal.configuration.UnknownRunConfiguration;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.ListBox;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbarPosition;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.RemoveAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.FoldoutLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.util.TextWithMnemonic;
import consulo.util.dataholder.Key;
import consulo.util.lang.function.Predicates;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;

/**
 * @author Vassiliy Kudryashov
 */
final class BeforeRunStepsPanel {
    private static final String EXPAND_PROPERTY_KEY = "ExpandBeforeRunStepsPanel";

    private final Runnable myChangeListener;
    private final MutableFlatDataModel<BeforeRunTask> myModel = FlatDataModel.of(List.of());
    private final List<BeforeRunTask> myOriginalTasks = new ArrayList<>();

    private RunConfiguration myRunConfiguration;
    private boolean myEditBeforeRun;

    private @Nullable FoldoutLayout myFoldout;
    private @Nullable ListBox<BeforeRunTask> myList;
    private @Nullable Component myListContainer;
    private @Nullable CheckBox myShowSettingsBox;

    @RequiredUIAccess
    BeforeRunStepsPanel(RunnerAndConfigurationSettings settings, Runnable changeListener) {
        myChangeListener = changeListener;
        myRunConfiguration = settings.getConfiguration();
        reset(settings);
    }

    @RequiredUIAccess
    Component createComponent() {
        FoldoutLayout foldout = myFoldout;
        if (foldout != null) {
            return foldout;
        }

        ListBox<BeforeRunTask> list = ListBox.create(myModel);
        list.setRender((presentation, item) -> {
            BeforeRunTask task = item.getValue();
            BeforeRunTaskProvider<BeforeRunTask> provider = task == null ? null : getProvider(myRunConfiguration, task);
            if (provider != null) {
                presentation.withIcon(provider.getTaskIcon(myRunConfiguration, task));
                presentation.append(provider.getDescription(task));
            }
        });
        list.setVisibleRowCount(7);
        myList = list;

        Component listContainer = ToolbarDecoratorBuilderFactory.getInstance()
            .create(list)
            .addOrReplaceAction(new StepAddAction())
            .addOrReplaceAction(new StepRemoveAction())
            .addOrReplaceAction(new StepEditAction())
            .addOrReplaceAction(new StepUpAction())
            .addOrReplaceAction(new StepDownAction())
            .withToolbarPosition(ActionToolbarPosition.RIGHT)
            .build();
        myListContainer = listContainer;

        CheckBox showSettingsBox = CheckBox.create(ExecutionLocalize.configurationEditBeforeRun());
        showSettingsBox.addValueListener(event -> {
            myEditBeforeRun = Boolean.TRUE.equals(event.getValue());
            updateTitle();
            myChangeListener.run();
        });
        myShowSettingsBox = showSettingsBox;

        DockLayout content = DockLayout.create();
        content.center(listContainer);
        content.bottom(showSettingsBox);

        foldout = FoldoutLayout.create(
            LocalizeValue.empty(),
            content,
            ApplicationPropertiesComponent.getInstance().getBoolean(EXPAND_PROPERTY_KEY, true)
        );
        foldout.addOpenedListener(
            event -> ApplicationPropertiesComponent.getInstance().setValue(EXPAND_PROPERTY_KEY, String.valueOf(event.isOpened()))
        );
        myFoldout = foldout;

        applyState();
        return foldout;
    }

    @RequiredUIAccess
    void reset(RunnerAndConfigurationSettings settings) {
        myRunConfiguration = settings.getConfiguration();

        myOriginalTasks.clear();
        myOriginalTasks.addAll(RunManagerImpl.getInstanceImpl(myRunConfiguration.getProject()).getBeforeRunTasks(myRunConfiguration));
        myModel.replaceAll(myOriginalTasks);
        myEditBeforeRun = settings.isEditBeforeRun();

        applyState();
    }

    List<BeforeRunTask> getTasks(boolean applyCurrentState) {
        if (applyCurrentState) {
            myOriginalTasks.clear();
            myOriginalTasks.addAll(getItems());
        }
        return Collections.unmodifiableList(myOriginalTasks);
    }

    boolean needEditBeforeRun() {
        return myEditBeforeRun;
    }

    @RequiredUIAccess
    void addTask(BeforeRunTask task) {
        myModel.add(task);
        onTasksChanged();
    }

    private List<BeforeRunTask> getItems() {
        List<BeforeRunTask> items = new ArrayList<>(myModel.getSize());
        for (int i = 0; i < myModel.getSize(); i++) {
            items.add(myModel.get(i));
        }
        return items;
    }

    @RequiredUIAccess
    private void applyState() {
        boolean unknown = myRunConfiguration instanceof UnknownRunConfiguration;

        CheckBox showSettingsBox = myShowSettingsBox;
        if (showSettingsBox != null) {
            showSettingsBox.setValue(myEditBeforeRun, false);
            showSettingsBox.setEnabled(!unknown);
        }

        Component listContainer = myListContainer;
        if (listContainer != null) {
            listContainer.setVisible(canAddTasks(false));
        }

        FoldoutLayout foldout = myFoldout;
        if (foldout != null) {
            foldout.setVisible(!unknown);
        }

        updateTitle();
    }

    @RequiredUIAccess
    private void updateTitle() {
        FoldoutLayout foldout = myFoldout;
        if (foldout != null) {
            foldout.setTitle(LocalizeValue.of(TextWithMnemonic.parse(buildTitle()).getText()));
        }
    }

    private String buildTitle() {
        StringBuilder sb = new StringBuilder();

        if (myEditBeforeRun) {
            sb.append(ExecutionLocalize.configurationEditBeforeRun().get());
        }

        List<BeforeRunTask> tasks = getItems();
        if (!tasks.isEmpty()) {
            SequencedMap<BeforeRunTaskProvider, Integer> counter = new LinkedHashMap<>();
            for (BeforeRunTask task : tasks) {
                BeforeRunTaskProvider<BeforeRunTask> provider = getProvider(myRunConfiguration, task);
                if (provider != null) {
                    Integer count = counter.get(provider);
                    if (count == null) {
                        count = task.getItemsCount();
                    }
                    else {
                        count += task.getItemsCount();
                    }
                    counter.put(provider, count);
                }
            }
            for (Map.Entry<BeforeRunTaskProvider, Integer> entry : counter.entrySet()) {
                LocalizeValue name = entry.getKey().getName();

                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(name.get());
                if (entry.getValue() > 1) {
                    sb.append(" (").append(entry.getValue().intValue()).append(")");
                }
            }
        }
        if (sb.length() > 0) {
            sb.insert(0, ": ");
        }
        sb.insert(0, ExecutionLocalize.beforeLaunchPanelTitle().get());
        return sb.toString();
    }

    private boolean canAddTasks(boolean checkOnlyAddAction) {
        RunConfiguration configuration = myRunConfiguration;
        if (configuration instanceof UnknownRunConfiguration) {
            return false;
        }
        Set<Key> activeProviderKeys = getActiveProviderKeys();
        return configuration.getProject().getExtensionPoint(BeforeRunTaskProvider.class).anyMatchSafe(
            provider -> provider.createTask(configuration) != null
                && (!checkOnlyAddAction || !provider.isSingleton() || !activeProviderKeys.contains(provider.getId()))
        );
    }

    private Set<Key> getActiveProviderKeys() {
        Set<Key> result = new HashSet<>();
        for (BeforeRunTask task : getItems()) {
            result.add(task.getProviderId());
        }
        return result;
    }

    @RequiredUIAccess
    private void onTasksChanged() {
        updateTitle();
        myChangeListener.run();
    }

    @RequiredUIAccess
    private void move(BeforeRunTask task, int delta) {
        int index = myModel.indexOf(task);
        int target = index + delta;
        if (index < 0 || target < 0 || target >= myModel.getSize()) {
            return;
        }

        myModel.remove(task);
        myModel.add(task, target);

        ListBox<BeforeRunTask> list = myList;
        if (list != null) {
            list.setValueByIndex(target);
        }
        onTasksChanged();
    }

    @RequiredUIAccess
    private ActionGroup createAddActions() {
        RunConfiguration configuration = myRunConfiguration;
        UIAccess uiAccess = configuration.getProject().getUIAccess();
        Set<Key> activeProviderKeys = getActiveProviderKeys();

        ActionGroup.Builder actionGroup = ActionGroup.newImmutableBuilder();
        configuration.getProject().getExtensionPoint(BeforeRunTaskProvider.class).forEach(each -> {
            @SuppressWarnings("unchecked")
            BeforeRunTaskProvider<BeforeRunTask> provider = each;
            if (provider.createTask(configuration) == null) {
                return;
            }
            if (activeProviderKeys.contains(provider.getId()) && provider.isSingleton()) {
                return;
            }
            actionGroup.add(new AnAction(provider.getName(), provider.getName(), provider.getIcon(configuration)) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(AnActionEvent e) {
                    BeforeRunTask task = provider.createTask(configuration);
                    if (task == null) {
                        return;
                    }

                    provider.configureTask(configuration, task).whenComplete((value, error) -> {
                        if (!provider.canExecuteTask(configuration, task)) {
                            return;
                        }
                        task.setEnabled(true);

                        Set<RunConfiguration> configurationSet = new HashSet<>();
                        collectRunBeforeRuns(task, configurationSet);
                        if (configurationSet.contains(configuration)) {
                            LocalizeValue warning = ExecutionLocalize.beforeLaunchPanelCyclic_dependency_warning(
                                configuration.getName(),
                                provider.getDescription(task)
                            );
                            uiAccess.give(() -> MessageBoxes.okWarning(warning)
                                .title(ExecutionLocalize.warningCommonTitle())
                                .showAsync(myFoldout));
                            return;
                        }
                        uiAccess.give(() -> addTask(task));
                    });
                }
            });
        });
        return actionGroup.build();
    }

    @SuppressWarnings("unchecked")
    private static @Nullable BeforeRunTaskProvider<BeforeRunTask> getProvider(RunConfiguration configuration, BeforeRunTask task) {
        return BeforeRunTaskProvider.getProvider(configuration.getProject(), task.getProviderId());
    }

    private static void collectRunBeforeRuns(BeforeRunTask task, Set<RunConfiguration> configurationSet) {
        if (task instanceof RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask runTask) {
            RunConfiguration configuration = runTask.getSettings().getConfiguration();

            List<BeforeRunTask> tasks = RunManagerImpl.getInstanceImpl(configuration.getProject()).getBeforeRunTasks(configuration);
            for (BeforeRunTask beforeRunTask : tasks) {
                if (beforeRunTask instanceof RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask configurableBeforeRunTask
                    && configurationSet.add(configurableBeforeRunTask.getSettings().getConfiguration())) {
                    collectRunBeforeRuns(beforeRunTask, configurationSet);
                }
            }
        }
    }

    private class StepAddAction extends AddAction<BeforeRunTask> {
        @Override
        @RequiredUIAccess
        protected void doAdd(AnActionEvent e) {
            if (myRunConfiguration instanceof UnknownRunConfiguration) {
                return;
            }

            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                ExecutionLocalize.addNewRunConfigurationAction2Name().get(),
                createAddActions(),
                e.getDataContext(),
                false,
                false,
                false,
                null,
                -1,
                Predicates.alwaysTrue()
            );
            popup.showUnderneathOf(e);
        }
    }

    private class StepRemoveAction extends RemoveAction<BeforeRunTask> {
        @Override
        @RequiredUIAccess
        protected void doRemove(BeforeRunTask task, AnActionEvent e) {
            myModel.remove(task);
            onTasksChanged();
        }
    }

    private class StepEditAction extends EditAction<BeforeRunTask> {
        @Override
        @RequiredUIAccess
        protected void doEdit(BeforeRunTask task, AnActionEvent e) {
            BeforeRunTaskProvider<BeforeRunTask> provider = getProvider(myRunConfiguration, task);
            if (provider == null || !provider.isConfigurable()) {
                return;
            }

            UIAccess uiAccess = myRunConfiguration.getProject().getUIAccess();
            provider.configureTask(myRunConfiguration, task).whenComplete((value, error) -> {
                if (error != null) {
                    return;
                }

                uiAccess.give(() -> {
                    myModel.update(task);
                    onTasksChanged();
                });
            });
        }
    }

    private class StepUpAction extends UpMoveAction<BeforeRunTask> {
        @Override
        @RequiredUIAccess
        protected void doUp(BeforeRunTask task, AnActionEvent e) {
            move(task, -1);
        }
    }

    private class StepDownAction extends DownMoveAction<BeforeRunTask> {
        @Override
        @RequiredUIAccess
        protected void doDown(BeforeRunTask task, AnActionEvent e) {
            move(task, 1);
        }
    }
}
