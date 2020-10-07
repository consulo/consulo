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

package com.intellij.execution.actions;

import com.intellij.execution.*;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.panels.NonOpaquePanel;
import consulo.actionSystem.ex.ComboBoxButton;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ExecutionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RunConfigurationsComboBoxAction extends ComboBoxAction implements DumbAware {
  @Deprecated
  public static final Image CHECKED_ICON = ImageEffects.resize(AllIcons.Actions.Checked, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
  @Deprecated
  public static final Image CHECKED_SELECTED_ICON = ImageEffects.resize(AllIcons.Actions.Checked_selected, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
  @Deprecated
  public static final Image EMPTY_ICON = Image.empty(Image.DEFAULT_ICON_SIZE);

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (ActionPlaces.isMainMenuOrActionSearch(e.getPlace())) {
      presentation.setDescriptionValue(ExecutionLocalize.chooseRunConfigurationActionDescription());
    }
    try {
      if (project == null || project.isDisposed() || !project.isInitialized()) {
        updatePresentation(null, null, null, presentation);
        presentation.setEnabled(false);
      }
      else {
        updatePresentation(ExecutionTargetManager.getActiveTarget(project), RunManagerEx.getInstanceEx(project).getSelectedConfiguration(), project, presentation);
        presentation.setEnabled(true);
      }
    }
    catch (IndexNotReadyException e1) {
      presentation.setEnabled(false);
    }
  }

  private static void updatePresentation(@Nullable ExecutionTarget target, @Nullable RunnerAndConfigurationSettings settings, @Nullable Project project, @Nonnull Presentation presentation) {
    if (project != null && target != null && settings != null) {
      String name = settings.getName();
      if (target != DefaultExecutionTarget.INSTANCE) {
        name += " | " + target.getDisplayName();
      }
      else {
        if (!settings.canRunOn(target)) {
          name += " | Nothing to run on";
        }
      }
      presentation.setText(name, false);
      presentation.putClientProperty(ComboBoxButton.LIKE_BUTTON, null);
      setConfigurationIcon(presentation, settings, project);
    }
    else {
      presentation.setTextValue(ExecutionLocalize.runComboBoxAddConfiguration());
      presentation.putClientProperty(ComboBoxButton.LIKE_BUTTON, (Runnable)() -> {
        ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_RUN_CONFIGURATIONS).actionPerformed(AnActionEvent.createFromDataContext("", null, DataManager.getInstance().getDataContext()));
      });
      presentation.setDescription(ActionsBundle.actionDescription(IdeActions.ACTION_EDIT_RUN_CONFIGURATIONS));
      presentation.setIcon(null);
    }
  }

  private static void setConfigurationIcon(final Presentation presentation, final RunnerAndConfigurationSettings settings, final Project project) {
    try {
      Image icon = RunManagerEx.getInstanceEx(project).getConfigurationIcon(settings);
      ExecutionManagerImpl executionManager = ExecutionManagerImpl.getInstance(project);
      List<RunContentDescriptor> runningDescriptors = executionManager.getRunningDescriptors(s -> s == settings);
      if (runningDescriptors.size() == 1) {
        icon = ExecutionUtil.getIconWithLiveIndicator(icon);
      }
      else if (runningDescriptors.size() > 1) {
        icon = ImageEffects.withText(icon, String.valueOf(runningDescriptors.size()));
      }
      presentation.setIcon(icon);
    }
    catch (IndexNotReadyException ignored) {
    }
  }

  @Override
  public boolean shouldShowDisabledActions() {
    return true;
  }

  @Nonnull
  @Override
  public JComponent createCustomComponent(final Presentation presentation) {
    ComboBoxButton button = createComboBoxButton(presentation);
    NonOpaquePanel panel = new NonOpaquePanel(new BorderLayout());
    panel.setBorder(IdeBorderFactory.createEmptyBorder(0, 0, 0, 2));
    panel.add(button.getComponent());
    return panel;
  }

  @Override
  @Nonnull
  public ActionGroup createPopupActionGroup(JComponent button) {
    final ActionGroup.Builder allActionsGroup = ActionGroup.newImmutableBuilder();
    final Project project = DataManager.getInstance().getDataContext(button).getData(CommonDataKeys.PROJECT);
    if (project != null) {
      final RunManagerEx runManager = RunManagerEx.getInstanceEx(project);

      allActionsGroup.add(ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_RUN_CONFIGURATIONS));
      allActionsGroup.add(new SaveTemporaryAction());
      allActionsGroup.addSeparator();

      RunnerAndConfigurationSettings selected = RunManager.getInstance(project).getSelectedConfiguration();
      if (selected != null) {
        ExecutionTarget activeTarget = ExecutionTargetManager.getActiveTarget(project);
        for (ExecutionTarget eachTarget : ExecutionTargetManager.getTargetsToChooseFor(project, selected)) {
          allActionsGroup.add(new SelectTargetAction(project, eachTarget, eachTarget.equals(activeTarget)));
        }
        allActionsGroup.addSeparator();
      }

      final List<ConfigurationType> types = runManager.getConfigurationFactories();
      for (ConfigurationType type : types) {
        final DefaultActionGroup actionGroup = new DefaultActionGroup();
        Map<String, List<RunnerAndConfigurationSettings>> structure = runManager.getStructure(type);
        for (Map.Entry<String, List<RunnerAndConfigurationSettings>> entry : structure.entrySet()) {
          DefaultActionGroup group = entry.getKey() != null ? new DefaultActionGroup(entry.getKey(), true) : actionGroup;
          group.getTemplatePresentation().setIcon(AllIcons.Nodes.Folder);
          for (RunnerAndConfigurationSettings settings : entry.getValue()) {
            group.add(new SelectConfigAction(settings, project));
          }
          if (group != actionGroup) {
            actionGroup.add(group);
          }
        }

        allActionsGroup.add(actionGroup);
        allActionsGroup.addSeparator();
      }
    }
    return allActionsGroup.build();
  }

  private static class SaveTemporaryAction extends DumbAwareAction {
    public SaveTemporaryAction() {
      Presentation presentation = getTemplatePresentation();
      presentation.setIcon(AllIcons.Actions.Menu_saveall);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(final AnActionEvent e) {
      final Project project = e.getData(CommonDataKeys.PROJECT);
      if (project != null) {
        RunnerAndConfigurationSettings settings = chooseTempSettings(project);
        if (settings != null) {
          final RunManager runManager = RunManager.getInstance(project);
          runManager.makeStable(settings);
        }
      }
    }

    @RequiredUIAccess
    @Override
    public void update(final AnActionEvent e) {
      final Presentation presentation = e.getPresentation();
      final Project project = e.getData(CommonDataKeys.PROJECT);
      if (project == null) {
        disable(presentation);
        return;
      }
      RunnerAndConfigurationSettings settings = chooseTempSettings(project);
      if (settings == null) {
        disable(presentation);
      }
      else {
        LocalizeValue textValue = ExecutionLocalize.saveTemporaryRunConfigurationActionName(settings.getName());

        presentation.setTextValue(textValue);
        presentation.setDescriptionValue(textValue.map(Presentation.NO_MNEMONIC));
        presentation.setVisible(true);
        presentation.setEnabled(true);
      }
    }

    private static void disable(final Presentation presentation) {
      presentation.setEnabled(false);
      presentation.setVisible(false);
    }

    @Nullable
    private static RunnerAndConfigurationSettings chooseTempSettings(@Nonnull Project project) {
      RunnerAndConfigurationSettings selectedConfiguration = RunManager.getInstance(project).getSelectedConfiguration();
      if (selectedConfiguration != null && selectedConfiguration.isTemporary()) {
        return selectedConfiguration;
      }
      Iterator<RunnerAndConfigurationSettings> iterator = RunManager.getInstance(project).getTempConfigurationsList().iterator();
      return iterator.hasNext() ? iterator.next() : null;
    }
  }

  private static class SelectTargetAction extends AnAction {
    private final Project myProject;
    private final ExecutionTarget myTarget;

    public SelectTargetAction(final Project project, final ExecutionTarget target, boolean selected) {
      myProject = project;
      myTarget = target;

      String name = target.getDisplayName();
      Presentation presentation = getTemplatePresentation();
      presentation.setText(name, false);
      presentation.setDescription("Select " + name);

      presentation.setIcon(selected ? ImageEffects.resize(AllIcons.Actions.Checked, Image.DEFAULT_ICON_SIZE) : Image.empty(Image.DEFAULT_ICON_SIZE));
      presentation.setSelectedIcon(selected ? ImageEffects.resize(AllIcons.Actions.Checked_selected, Image.DEFAULT_ICON_SIZE) : Image.empty(Image.DEFAULT_ICON_SIZE));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
      ExecutionTargetManager.setActiveTarget(myProject, myTarget);
      updatePresentation(ExecutionTargetManager.getActiveTarget(myProject), RunManagerEx.getInstanceEx(myProject).getSelectedConfiguration(), myProject, e.getPresentation());
    }
  }

  private static class SelectConfigAction extends DumbAwareAction {
    private final RunnerAndConfigurationSettings myConfiguration;
    private final Project myProject;

    public SelectConfigAction(final RunnerAndConfigurationSettings configuration, final Project project) {
      myConfiguration = configuration;
      myProject = project;
      String name = configuration.getName();
      if (name == null || name.length() == 0) {
        name = " ";
      }
      final Presentation presentation = getTemplatePresentation();
      presentation.setText(name, false);
      final ConfigurationType type = configuration.getType();
      if (type != null) {
        presentation.setDescription("Select " + type.getConfigurationTypeDescription() + " '" + name + "'");
      }
      updateIcon(presentation);
    }

    private void updateIcon(final Presentation presentation) {
      setConfigurationIcon(presentation, myConfiguration, myProject);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(final AnActionEvent e) {
      RunManager.getInstance(myProject).setSelectedConfiguration(myConfiguration);
      updatePresentation(ExecutionTargetManager.getActiveTarget(myProject), myConfiguration, myProject, e.getPresentation());
    }

    @RequiredUIAccess
    @Override
    public void update(final AnActionEvent e) {
      super.update(e);
      updateIcon(e.getPresentation());
    }
  }
}
