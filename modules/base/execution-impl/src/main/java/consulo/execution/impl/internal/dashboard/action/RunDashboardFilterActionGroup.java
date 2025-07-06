// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.execution.dashboard.RunDashboardManager;
import consulo.execution.dashboard.RunDashboardRunConfigurationStatus;
import consulo.execution.impl.internal.dashboard.RunDashboardManagerImpl;
import consulo.execution.impl.internal.dashboard.RunDashboardServiceViewContributor;
import consulo.execution.impl.internal.dashboard.tree.RunDashboardStatusFilter;
import consulo.execution.impl.internal.service.action.ServiceViewFilterGroup;
import consulo.execution.service.ServiceViewActionUtils;
import consulo.execution.service.ServiceViewContributor;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CheckedActionGroup;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.annotation.Nullable;

import java.util.Set;

import static consulo.execution.dashboard.RunDashboardRunConfigurationStatus.*;

@ActionImpl(id = "RunDashboard.Filter", parents = @ActionParentRef(@ActionRef(type = ServiceViewFilterGroup.class)))
public final class RunDashboardFilterActionGroup extends DefaultActionGroup implements CheckedActionGroup, DumbAware {

  @Inject
  public RunDashboardFilterActionGroup() {
    this(null, false);
  }

  RunDashboardFilterActionGroup(@Nullable String shortName, boolean popup) {
    super(shortName, popup);
    RunDashboardRunConfigurationStatus[] statuses = new RunDashboardRunConfigurationStatus[]{STARTED, FAILED, STOPPED, CONFIGURED};
    for (RunDashboardRunConfigurationStatus status : statuses) {
      add(new RunDashboardStatusFilterToggleAction(status));
    }
  }

//  @Override
//  public @NotNull ActionUpdateThread getActionUpdateThread() {
//    return ActionUpdateThread.BGT;
//  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Set<ServiceViewContributor> contributors = e.getData(ServiceViewActionUtils.CONTRIBUTORS_KEY);

    boolean isEnabled = false;
    if (contributors != null) {
      for (ServiceViewContributor contributor : contributors) {
        if (contributor instanceof RunDashboardServiceViewContributor) {
          isEnabled = true;
          break;
        }
      }
    }

    e.getPresentation().setEnabledAndVisible(isEnabled);
  }

  @Override
  public boolean isPopup() {
    return true;
  }

  private static final class RunDashboardStatusFilterToggleAction extends ToggleAction implements DumbAware {
    private final RunDashboardRunConfigurationStatus myStatus;

    RunDashboardStatusFilterToggleAction(RunDashboardRunConfigurationStatus status) {
      super(status.getName());
      myStatus = status;
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      Project project = e.getData(Project.KEY);
      if (project == null) return false;

      RunDashboardStatusFilter statusFilter = ((RunDashboardManagerImpl)RunDashboardManager.getInstance(project)).getStatusFilter();
      return statusFilter.isVisible(myStatus);
    }

//    @Override
//    public @NotNull ActionUpdateThread getActionUpdateThread() {
//      return ActionUpdateThread.EDT;
//    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      Project project = e.getData(Project.KEY);
      if (project == null) return;

      RunDashboardManagerImpl manager = (RunDashboardManagerImpl)RunDashboardManager.getInstance(project);
      RunDashboardStatusFilter statusFilter = manager.getStatusFilter();
      if (state) {
        statusFilter.show(myStatus);
      }
      else {
        statusFilter.hide(myStatus);
      }
      manager.updateDashboard(true);
    }
  }
}
