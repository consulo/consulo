/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.impl.internal.compound;

import consulo.configurable.ConfigurationException;
import consulo.execution.BeforeRunTask;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.impl.internal.RunConfigurationBeforeRunProvider;
import consulo.execution.impl.internal.RunConfigurationSelector;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.impl.internal.configuration.UnknownConfigurationType;
import consulo.language.localize.LanguageLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.ListBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbarPosition;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListSeparator;
import consulo.ui.ex.popup.MultiSelectionListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.image.Image;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompoundRunConfigurationSettingsEditor extends SettingsEditor<CompoundRunConfiguration> {
    private final RunManagerImpl myRunManager;
    private final MutableFlatDataModel<RunConfiguration> myModel = FlatDataModel.of(List.of());
    private @Nullable ListBox<RunConfiguration> myList;
    private @Nullable CompoundRunConfiguration mySnapshot;

    public CompoundRunConfigurationSettingsEditor(Project project) {
        myRunManager = RunManagerImpl.getInstanceImpl(project);
    }

    private boolean canBeAdded(RunConfiguration candidate, CompoundRunConfiguration root) {
        if (candidate.getType() == root.getType() && candidate.getName().equals(root.getName())) {
            return false;
        }
        List<BeforeRunTask> tasks = myRunManager.getBeforeRunTasks(candidate);
        for (BeforeRunTask task : tasks) {
            if (task instanceof RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask runTask) {
                RunnerAndConfigurationSettings settings = runTask.getSettings();
                if (settings != null && !canBeAdded(settings.getConfiguration(), root)) {
                    return false;
                }
            }
        }
        if (candidate instanceof CompoundRunConfiguration compoundRunConfiguration) {
            for (RunConfiguration configuration : compoundRunConfiguration.getSetToRun()) {
                if (!canBeAdded(configuration, root)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void resetEditorFrom(CompoundRunConfiguration compoundRunConfiguration) {
        mySnapshot = compoundRunConfiguration;
        setConfigurations(compoundRunConfiguration.getSetToRun());
    }

    @Override
    protected void applyEditorTo(CompoundRunConfiguration s) throws ConfigurationException {
        Set<RunConfiguration> checked = new HashSet<>();
        for (RunConfiguration configuration : getConfigurations()) {
            LocalizeValue message =
                LanguageLocalize.compoundRunConfigurationCycle(configuration.getType().getDisplayName(), configuration.getName());
            if (!canBeAdded(configuration, s)) {
                throw new ConfigurationException(message);
            }
            checked.add(configuration);
        }
        Set<RunConfiguration> toRun = s.getSetToRun();
        toRun.clear();
        toRun.addAll(checked);
    }

    private List<RunConfiguration> getConfigurations() {
        List<RunConfiguration> configurations = new ArrayList<>(myModel.getSize());
        for (int i = 0; i < myModel.getSize(); i++) {
            configurations.add(myModel.get(i));
        }
        return configurations;
    }

    private void setConfigurations(Iterable<RunConfiguration> configurations) {
        List<RunConfiguration> sorted = new ArrayList<>();
        for (RunConfiguration configuration : configurations) {
            sorted.add(configuration);
        }
        sorted.sort(CompoundRunConfiguration.COMPARATOR);
        myModel.replaceAll(sorted);
    }

    @RequiredUIAccess
    @Override
    protected Component createUIComponent() {
        ListBox<RunConfiguration> list = ListBox.create(myModel);
        list.setRender((presentation, item) -> {
            RunConfiguration configuration = item.getValue();
            if (configuration != null) {
                presentation.withIcon(configuration.getType().getIcon());
                presentation.append(configuration.getType().getDisplayName().get() + " '" + configuration.getName() + "'");
            }
        });
        list.setVisibleRowCount(15);
        myList = list;

        return ToolbarDecoratorBuilderFactory.getInstance()
            .create(list)
            .addOrReplaceAction(new AddConfigurationAction())
            .addOrReplaceAction(new EditConfigurationAction())
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .withToolbarPosition(ActionToolbarPosition.TOP)
            .build();
    }

    private class AddConfigurationAction extends AddAction<RunConfiguration> {
        @Override
        @RequiredUIAccess
        protected void doAdd(AnActionEvent e) {
            CompoundRunConfiguration snapshot = mySnapshot;
            if (snapshot == null) {
                return;
            }

            List<RunConfiguration> current = getConfigurations();
            List<RunConfiguration> configurations = new ArrayList<>();
            for (ConfigurationType type : myRunManager.getConfigurationFactories()) {
                if (!(type instanceof UnknownConfigurationType)) {
                    for (RunnerAndConfigurationSettings settings : myRunManager.getConfigurationSettingsList(type)) {
                        RunConfiguration configuration = settings.getConfiguration();
                        if (!current.contains(configuration) && canBeAdded(configuration, snapshot)) {
                            configurations.add(configuration);
                        }
                    }
                }
            }

            JBPopupFactory.getInstance().createListPopup(new MultiSelectionListPopupStep<>(null, configurations) {
                @Override
                public @Nullable ListSeparator getSeparatorAbove(RunConfiguration value) {
                    int i = configurations.indexOf(value);
                    if (i < 1) {
                        return null;
                    }
                    RunConfiguration previous = configurations.get(i - 1);
                    return value.getType() != previous.getType() ? new ListSeparator() : null;
                }

                @Override
                public Image getIconFor(RunConfiguration value) {
                    return value.getType().getIcon();
                }

                @Override
                public boolean isSpeedSearchEnabled() {
                    return true;
                }

                @Override
                public String getTextFor(RunConfiguration value) {
                    return value.getName();
                }

                @Override
                @RequiredUIAccess
                public PopupStep<?> onChosen(List<RunConfiguration> selectedValues, boolean finalChoice) {
                    List<RunConfiguration> newConfigurations = getConfigurations();
                    newConfigurations.addAll(selectedValues);
                    setConfigurations(newConfigurations);

                    ListBox<RunConfiguration> list = myList;
                    if (list != null && !selectedValues.isEmpty()) {
                        list.setValue(selectedValues.get(0));
                    }
                    fireEditorStateChanged();
                    return FINAL_CHOICE;
                }
            }).showUnderneathOf(e);
        }
    }

    private class EditConfigurationAction extends EditAction<RunConfiguration> {
        @Override
        @RequiredUIAccess
        protected void doEdit(RunConfiguration configuration, AnActionEvent e) {
            RunConfigurationSelector selector = e.getData(RunConfigurationSelector.KEY);
            if (selector != null) {
                selector.select(configuration);
            }
        }
    }
}
