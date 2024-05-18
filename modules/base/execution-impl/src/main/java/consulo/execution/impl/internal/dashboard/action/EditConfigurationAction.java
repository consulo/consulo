// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.ExecutionBundle;
import consulo.execution.RunConfigurationEditor;
import consulo.execution.RunManager;
import consulo.execution.dashboard.RunDashboardRunConfigurationNode;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ActionImpl(id = "RunDashboard.EditConfiguration")
public final class EditConfigurationAction extends AnAction {

//  @Override
//  public @NotNull ActionUpdateThread getActionUpdateThread() {
//    return ActionUpdateThread.BGT;
//  }


  @Nullable
  @Override
  protected Image getTemplateIcon() {
    return PlatformIconGroup.actionsEditsource();
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    RunDashboardRunConfigurationNode node = project == null ? null : RunDashboardActionUtils.getTarget(e);
    boolean enabled = node != null && RunManager.getInstance(project).hasSettings(node.getConfigurationSettings());
    Presentation presentation = e.getPresentation();
    presentation.setEnabled(enabled);
    boolean popupPlace = ActionPlaces.isPopupPlace(e.getPlace());
    presentation.setVisible(enabled || !popupPlace);
    if (popupPlace) {
      presentation.setText(getTemplatePresentation().getText() + "...");
    }
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    RunDashboardRunConfigurationNode node = project == null ? null : RunDashboardActionUtils.getTarget(e);
    if (node == null) return;

    RunConfigurationEditor.getInstance(project).editConfiguration(project, node.getConfigurationSettings(),
                                                                  ExecutionBundle.message("run.dashboard.edit.configuration.dialog.title"));
  }
}
