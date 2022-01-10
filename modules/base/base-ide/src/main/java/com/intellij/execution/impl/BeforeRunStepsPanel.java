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
package com.intellij.execution.impl;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.UnknownRunConfiguration;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author Vassiliy Kudryashov
 */
class BeforeRunStepsPanel {
  private final JPanel myPanel;
  private final JCheckBox myShowSettingsBeforeRunCheckBox;
  private final JBList<BeforeRunTask> myList;
  private final CollectionListModel<BeforeRunTask> myModel;
  private RunConfiguration myRunConfiguration;

  private final List<BeforeRunTask> originalTasks = new ArrayList<>();
  private StepsBeforeRunListener myListener;
  private final JPanel myListContainer;

  BeforeRunStepsPanel(StepsBeforeRunListener listener) {
    myListener = listener;
    myModel = new CollectionListModel<>();
    myPanel = new JPanel(new BorderLayout());
    myList = new JBList<>(myModel);
    myList.getEmptyText().setText(ExecutionBundle.message("before.launch.panel.empty"));
    myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myList.setCellRenderer(new MyListCellRenderer());

    myModel.addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
        adjustVisibleRowCount();
        updateText();
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
        adjustVisibleRowCount();
        updateText();
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
      }

      private void adjustVisibleRowCount() {
        myList.setVisibleRowCount(Math.max(4, Math.min(8, myModel.getSize())));
      }
    });

    ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myList);
    decorator.setToolbarPosition(ActionToolbarPosition.RIGHT);
    decorator.setEditAction(button -> {
      int index = myList.getSelectedIndex();
      if (index == -1) return;
      Pair<BeforeRunTask, BeforeRunTaskProvider<BeforeRunTask>> selection = getSelection();
      if (selection == null) return;
      BeforeRunTask task = selection.getFirst();
      BeforeRunTaskProvider<BeforeRunTask> provider = selection.getSecond();
      provider.configureTask(myRunConfiguration, task).doWhenDone(() -> {
        myModel.setElementAt(task, index);
        updateText();
      });
    });
    decorator.setEditActionUpdater(e -> {
      Pair<BeforeRunTask, BeforeRunTaskProvider<BeforeRunTask>> selection = getSelection();
      return selection != null && selection.getSecond().isConfigurable();
    });
    decorator.setAddAction(button -> doAddAction(button));
    decorator.setAddActionUpdater(e -> checkBeforeRunTasksAbility(true));

    myShowSettingsBeforeRunCheckBox = new JCheckBox(ExecutionBundle.message("configuration.edit.before.run"));
    myShowSettingsBeforeRunCheckBox.addActionListener(e -> updateText());

    myListContainer = decorator.createPanel();

    myPanel.add(myListContainer, BorderLayout.CENTER);
    myPanel.add(myShowSettingsBeforeRunCheckBox, BorderLayout.SOUTH);
  }

  @Nullable
  private Pair<BeforeRunTask, BeforeRunTaskProvider<BeforeRunTask>> getSelection() {
    final int index = myList.getSelectedIndex();
    if (index == -1) return null;
    BeforeRunTask task = myModel.getElementAt(index);
    Key providerId = task.getProviderId();
    BeforeRunTaskProvider<BeforeRunTask> provider = BeforeRunTaskProvider.getProvider(myRunConfiguration.getProject(), providerId);
    return provider != null ? Pair.create(task, provider) : null;
  }

  void doReset(RunnerAndConfigurationSettings settings) {
    myRunConfiguration = settings.getConfiguration();

    originalTasks.clear();
    RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(myRunConfiguration.getProject());
    originalTasks.addAll(runManager.getBeforeRunTasks(myRunConfiguration));
    myModel.replaceAll(originalTasks);
    myShowSettingsBeforeRunCheckBox.setSelected(settings.isEditBeforeRun());
    myShowSettingsBeforeRunCheckBox.setEnabled(!(isUnknown()));
    myListContainer.setVisible(checkBeforeRunTasksAbility(false));
    updateText();
  }

  private void updateText() {
    StringBuilder sb = new StringBuilder();

    if (myShowSettingsBeforeRunCheckBox.isSelected()) {
      sb.append(ExecutionBundle.message("configuration.edit.before.run"));
    }

    List<BeforeRunTask> tasks = myModel.getItems();
    if (!tasks.isEmpty()) {
      LinkedHashMap<BeforeRunTaskProvider, Integer> counter = new LinkedHashMap<>();
      for (BeforeRunTask task : tasks) {
        BeforeRunTaskProvider<BeforeRunTask> provider = BeforeRunTaskProvider.getProvider(myRunConfiguration.getProject(), task.getProviderId());
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
      for (Iterator<Map.Entry<BeforeRunTaskProvider, Integer>> iterator = counter.entrySet().iterator(); iterator.hasNext(); ) {
        Map.Entry<BeforeRunTaskProvider, Integer> entry = iterator.next();
        BeforeRunTaskProvider provider = entry.getKey();
        String name = provider.getName();
        if (name.startsWith("Run ")) {
          name = name.substring(4);
        }
        if (sb.length() > 0) {
          sb.append(", ");
        }
        sb.append(name);
        if (entry.getValue() > 1) {
          sb.append(" (").append(entry.getValue().intValue()).append(")");
        }
      }
    }
    if (sb.length() > 0) {
      sb.insert(0, ": ");
    }
    sb.insert(0, ExecutionBundle.message("before.launch.panel.title"));
    myListener.titleChanged(sb.toString());
  }

  public List<BeforeRunTask> getTasks(boolean applyCurrentState) {
    if (applyCurrentState) {
      originalTasks.clear();
      originalTasks.addAll(myModel.getItems());
    }
    return Collections.unmodifiableList(originalTasks);
  }

  public boolean needEditBeforeRun() {
    return myShowSettingsBeforeRunCheckBox.isSelected();
  }

  private boolean checkBeforeRunTasksAbility(boolean checkOnlyAddAction) {
    if (isUnknown()) {
      return false;
    }
    Set<Key> activeProviderKeys = getActiveProviderKeys();
    final List<BeforeRunTaskProvider<BeforeRunTask>> providers = BeforeRunTaskProvider.EP_NAME.getExtensionList(myRunConfiguration.getProject());
    for (final BeforeRunTaskProvider<BeforeRunTask> provider : providers) {
      if (provider.createTask(myRunConfiguration) != null) {
        if (!checkOnlyAddAction) {
          return true;
        }
        else if (!provider.isSingleton() || !activeProviderKeys.contains(provider.getId())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isUnknown() {
    return myRunConfiguration instanceof UnknownRunConfiguration;
  }

  void doAddAction(AnActionButton button) {
    if (isUnknown()) {
      return;
    }

    final JBPopupFactory popupFactory = JBPopupFactory.getInstance();
    final List<BeforeRunTaskProvider<BeforeRunTask>> providers = BeforeRunTaskProvider.EP_NAME.getExtensionList(myRunConfiguration.getProject());
    Set<Key> activeProviderKeys = getActiveProviderKeys();

    ActionGroup.Builder actionGroup = ActionGroup.newImmutableBuilder();
    for (final BeforeRunTaskProvider<BeforeRunTask> provider : providers) {
      if (provider.createTask(myRunConfiguration) == null) continue;
      if (activeProviderKeys.contains(provider.getId()) && provider.isSingleton()) continue;
      AnAction providerAction = new AnAction(LocalizeValue.of(provider.getName()), LocalizeValue.empty(), provider.getIcon()) {
        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
          BeforeRunTask task = provider.createTask(myRunConfiguration);
          if (task == null) {
            return;
          }

          provider.configureTask(myRunConfiguration, task).doWhenProcessed(() -> {
            if (!provider.canExecuteTask(myRunConfiguration, task)) return;
            task.setEnabled(true);

            Set<RunConfiguration> configurationSet = new HashSet<>();
            getAllRunBeforeRuns(task, configurationSet);
            if (configurationSet.contains(myRunConfiguration)) {
              JOptionPane.showMessageDialog(myPanel,
                                            ExecutionBundle.message("before.launch.panel.cyclic_dependency_warning", myRunConfiguration.getName(), provider.getDescription(task)),
                                            ExecutionBundle.message("warning.common.title"), JOptionPane.WARNING_MESSAGE);
              return;
            }
            addTask(task);
            myListener.fireStepsBeforeRunChanged();
          });
        }
      };
      actionGroup.add(providerAction);
    }

    DataContext dataContext = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, myRunConfiguration.getProject())
            .add(PlatformDataKeys.CONTEXT_COMPONENT, myPanel)
            .build();

    final ListPopup popup = popupFactory .createActionGroupPopup(ExecutionBundle.message("add.new.run.configuration.acrtion.name"), actionGroup.build(), dataContext, false, false, false, null, -1, Conditions.alwaysTrue());
    popup.show(button.getPreferredPopupPoint());
  }

  public JPanel getPanel() {
    return myPanel;
  }

  public void addTask(BeforeRunTask task) {
    myModel.add(task);
  }

  private Set<Key> getActiveProviderKeys() {
    Set<Key> result = new HashSet<>();
    for (BeforeRunTask task : myModel.getItems()) {
      result.add(task.getProviderId());
    }
    return result;
  }

  private void getAllRunBeforeRuns(BeforeRunTask task, Set<RunConfiguration> configurationSet) {
    if (task instanceof RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask) {
      RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask runTask = (RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask)task;
      RunConfiguration configuration = runTask.getSettings().getConfiguration();

      List<BeforeRunTask> tasks = RunManagerImpl.getInstanceImpl(configuration.getProject()).getBeforeRunTasks(configuration);
      for (BeforeRunTask beforeRunTask : tasks) {
        if (beforeRunTask instanceof RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask) {
          if (configurationSet.add(((RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask)beforeRunTask).getSettings().getConfiguration())) {
            getAllRunBeforeRuns(beforeRunTask, configurationSet);
          }
        }
      }
    }
  }

  interface StepsBeforeRunListener {
    void fireStepsBeforeRunChanged();

    void titleChanged(String title);
  }

  private class MyListCellRenderer extends ColoredListCellRenderer<BeforeRunTask> {
    @Override
    protected void customizeCellRenderer(@Nonnull JList<? extends BeforeRunTask> list, BeforeRunTask value, int index, boolean selected, boolean hasFocus) {
      BeforeRunTaskProvider<BeforeRunTask> provider = BeforeRunTaskProvider.getProvider(myRunConfiguration.getProject(), value.getProviderId());
      if (provider != null) {
        setIcon(provider.getTaskIcon(value));
        append(provider.getDescription(value));
      }
    }
  }
}
