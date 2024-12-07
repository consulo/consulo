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

import consulo.dataContext.DataContext;
import consulo.execution.BeforeRunTask;
import consulo.execution.BeforeRunTaskProvider;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.impl.internal.RunConfigurationBeforeRunProvider;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.impl.internal.configuration.UnknownRunConfiguration;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbarPosition;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.function.Conditions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
    myList.getEmptyText().setText(ExecutionLocalize.beforeLaunchPanelEmpty().get());
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

    myShowSettingsBeforeRunCheckBox = new JCheckBox(ExecutionLocalize.configurationEditBeforeRun().get());
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
      sb.append(ExecutionLocalize.configurationEditBeforeRun());
    }

    List<BeforeRunTask> tasks = myModel.getItems();
    if (!tasks.isEmpty()) {
      LinkedHashMap<BeforeRunTaskProvider, Integer> counter = new LinkedHashMap<>();
      for (BeforeRunTask task : tasks) {
        BeforeRunTaskProvider<BeforeRunTask> provider =
          BeforeRunTaskProvider.getProvider(myRunConfiguration.getProject(), task.getProviderId());
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
    sb.insert(0, ExecutionLocalize.beforeLaunchPanelTitle());
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
    final List<BeforeRunTaskProvider> providers = BeforeRunTaskProvider.EP_NAME.getExtensionList(myRunConfiguration.getProject());
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
    final List<BeforeRunTaskProvider> providers = BeforeRunTaskProvider.EP_NAME.getExtensionList(myRunConfiguration.getProject());
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
              JOptionPane.showMessageDialog(
                myPanel,
                ExecutionLocalize.beforeLaunchPanelCyclic_dependency_warning(
                  myRunConfiguration.getName(),
                  provider.getDescription(task)
                ).get(),
                ExecutionLocalize.warningCommonTitle().get(),
                JOptionPane.WARNING_MESSAGE
              );
              return;
            }
            addTask(task);
            myListener.fireStepsBeforeRunChanged();
          });
        }
      };
      actionGroup.add(providerAction);
    }

    DataContext dataContext = DataContext.builder()
      .add(Project.KEY, myRunConfiguration.getProject())
      .add(UIExAWTDataKey.CONTEXT_COMPONENT, myPanel)
      .build();

    final ListPopup popup = popupFactory.createActionGroupPopup(
      ExecutionLocalize.addNewRunConfigurationAction2Name().get(),
      actionGroup.build(),
      dataContext,
      false,
      false,
      false,
      null,
      -1,
      Conditions.alwaysTrue()
    );
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
    protected void customizeCellRenderer(
      @Nonnull JList<? extends BeforeRunTask> list,
      BeforeRunTask value,
      int index,
      boolean selected,
      boolean hasFocus
    ) {
      BeforeRunTaskProvider<BeforeRunTask> provider =
        BeforeRunTaskProvider.getProvider(myRunConfiguration.getProject(), value.getProviderId());
      if (provider != null) {
        setIcon(provider.getTaskIcon(value));
        append(provider.getDescription(value));
      }
    }
  }
}
