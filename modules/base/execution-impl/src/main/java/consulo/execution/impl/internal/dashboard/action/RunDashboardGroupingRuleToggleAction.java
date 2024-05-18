// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard.action;

import consulo.application.dumb.DumbAware;
import consulo.execution.impl.internal.dashboard.RunDashboardServiceViewContributor;
import consulo.execution.service.ServiceEventListener;
import consulo.execution.service.ServiceViewActionUtils;
import consulo.execution.service.ServiceViewContributor;
import consulo.execution.service.ServiceViewOptions;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;

import java.util.Set;

abstract class RunDashboardGroupingRuleToggleAction extends ToggleAction implements DumbAware {

//  @Override
//  public @NotNull ActionUpdateThread getActionUpdateThread() {
//    return ActionUpdateThread.BGT;
//  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    ServiceViewOptions viewOptions = e.getData(ServiceViewActionUtils.OPTIONS_KEY);
    Presentation presentation = e.getPresentation();
    if (viewOptions != null && !viewOptions.isGroupByServiceGroups()) {
      presentation.setEnabledAndVisible(false);
      return;
    }
    Set<ServiceViewContributor> contributors = e.getData(ServiceViewActionUtils.CONTRIBUTORS_KEY);
    if (contributors != null) {
      for (ServiceViewContributor contributor : contributors) {
        if (contributor instanceof RunDashboardServiceViewContributor) {
          presentation.setEnabledAndVisible(true);
          return;
        }
      }
    }
    presentation.setEnabledAndVisible(false);
  }

  @Override
  public boolean isSelected(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) return false;

    return ProjectPropertiesComponent.getInstance(project).getBoolean(getRuleName(), isEnabledByDefault());
  }

  @Override
  public void setSelected(@Nonnull AnActionEvent e, boolean state) {
    Project project = e.getData(Project.KEY);
    if (project == null) return;

    ProjectPropertiesComponent.getInstance(project).setValue(getRuleName(), state, isEnabledByDefault());
    project.getMessageBus().syncPublisher(ServiceEventListener.TOPIC).handle(
      ServiceEventListener.ServiceEvent.createResetEvent(RunDashboardServiceViewContributor.class));
  }

  protected abstract @Nonnull String getRuleName();

  protected boolean isEnabledByDefault() {
    return true;
  }
}
