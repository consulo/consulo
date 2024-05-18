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
import consulo.dataContext.DataManager;
import consulo.execution.BeforeRunTask;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.impl.internal.RunConfigurationBeforeRunProvider;
import consulo.execution.impl.internal.RunConfigurationSelector;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.impl.internal.configuration.UnknownConfigurationType;
import consulo.language.LangBundle;
import consulo.project.Project;
import consulo.ui.ex.action.ActionToolbarPosition;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListSeparator;
import consulo.ui.ex.popup.MultiSelectionListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompoundRunConfigurationSettingsEditor extends SettingsEditor<CompoundRunConfiguration> {
  private final JBList myList;
  private final RunManagerImpl myRunManager;
  private final SortedListModel<RunConfiguration> myModel;
  private CompoundRunConfiguration mySnapshot;


  public CompoundRunConfigurationSettingsEditor(@Nonnull Project project) {
    myRunManager = RunManagerImpl.getInstanceImpl(project);
    myModel = new SortedListModel<>(CompoundRunConfiguration.COMPARATOR);
    myList = new JBList(myModel);
    myList.setCellRenderer(new ColoredListCellRenderer() {
      @Override
      protected void customizeCellRenderer(@Nonnull JList list, Object value, int index, boolean selected, boolean hasFocus) {
        RunConfiguration configuration = myModel.get(index);
        setIcon(configuration.getType().getIcon());
        append(configuration.getType().getDisplayName() + " '" + configuration.getName() + "'");
      }
    });
    myList.setVisibleRowCount(15);
  }

  private boolean canBeAdded(@Nonnull RunConfiguration candidate, @Nonnull final CompoundRunConfiguration root) {
    if (candidate.getType() == root.getType() && candidate.getName().equals(root.getName())) return false;
    List<BeforeRunTask> tasks = myRunManager.getBeforeRunTasks(candidate);
    for (BeforeRunTask task : tasks) {
      if (task instanceof RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask) {
        RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask runTask
                = (RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask)task;
        RunnerAndConfigurationSettings settings = runTask.getSettings();
        if (settings != null) {
          if (!canBeAdded(settings.getConfiguration(), root)) return false;
        }
      }
    }
    if (candidate instanceof CompoundRunConfiguration) {
      Set<RunConfiguration> set = ((CompoundRunConfiguration)candidate).getSetToRun();
      for (RunConfiguration configuration : set) {
        if (!canBeAdded(configuration, root)) return false;
      }
    }
    return true;
  }

  @Override
  protected void resetEditorFrom(CompoundRunConfiguration compoundRunConfiguration) {
    myModel.clear();
    myModel.addAll(compoundRunConfiguration.getSetToRun());
    mySnapshot = compoundRunConfiguration;
  }

  @Override
  protected void applyEditorTo(CompoundRunConfiguration s) throws ConfigurationException {
    Set<RunConfiguration> checked = new HashSet<>();
    for (int i = 0; i < myModel.getSize(); i++) {
      RunConfiguration configuration = myModel.get(i);
      String message =
              LangBundle.message("compound.run.configuration.cycle", configuration.getType().getDisplayName(), configuration.getName());
      if (!canBeAdded(configuration, s)) throw new ConfigurationException(message);
      checked.add(configuration);
    }
    Set<RunConfiguration> toRun = s.getSetToRun();
    toRun.clear();
    toRun.addAll(checked);
  }

  @Nonnull
  @Override
  protected JComponent createEditor() {
    final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myList);
    return decorator.disableUpDownActions().setAddAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {

        final List<RunConfiguration> all = new ArrayList<>();
        for (ConfigurationType type : myRunManager.getConfigurationFactories()) {
          if (!(type instanceof UnknownConfigurationType)) {
            for (RunnerAndConfigurationSettings settings : myRunManager.getConfigurationSettingsList(type)) {
              all.add(settings.getConfiguration());
            }
          }
        }

        final List<RunConfiguration> configurations = ContainerUtil.filter(all,
                                                                           configuration -> !mySnapshot.getSetToRun().contains(configuration) && canBeAdded(configuration, mySnapshot));
        JBPopupFactory.getInstance().createListPopup(new MultiSelectionListPopupStep<RunConfiguration>(null, configurations){
          @Nullable
          @Override
          public ListSeparator getSeparatorAbove(RunConfiguration value) {
            int i = configurations.indexOf(value);
            if (i <1) return null;
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

          @Nonnull
          @Override
          public String getTextFor(RunConfiguration value) {
            return value.getName();
          }

          @Override
          public PopupStep<?> onChosen(List<RunConfiguration> selectedValues, boolean finalChoice) {
            myList.clearSelection();
            myModel.addAll(selectedValues);
            return FINAL_CHOICE;
          }

        }).showUnderneathOf(decorator.getActionsPanel());
      }
    }).setEditAction(e -> {
      int index = myList.getSelectedIndex();
      if (index == -1) return;
      RunConfiguration configuration = myModel.get(index);
      RunConfigurationSelector
        selector = DataManager.getInstance().getDataContext(e.getContextComponent()).getData(RunConfigurationSelector.KEY);
      if (selector != null) {
        selector.select(configuration);
      }
    }).setToolbarPosition(ActionToolbarPosition.TOP).createPanel();
  }
}
