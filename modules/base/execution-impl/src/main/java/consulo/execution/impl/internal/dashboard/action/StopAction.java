// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.dashboard.RunDashboardRunConfigurationNode;
import consulo.execution.impl.internal.ExecutionManagerImpl;
import consulo.execution.impl.internal.ui.RunContentManagerImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.content.Content;
import consulo.ui.image.Image;
import consulo.util.collection.JBIterable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static consulo.execution.impl.internal.dashboard.action.RunDashboardActionUtils.getLeafTargets;

/**
 * @author konstantin.aleev
 */
@ActionImpl(id = "RunDashboard.Stop")
public final class StopAction extends DumbAwareAction {

//  @Override
//  public @NotNull ActionUpdateThread getActionUpdateThread() {
//    return ActionUpdateThread.BGT;
//  }


  @Nullable
  @Override
  protected Image getTemplateIcon() {
    return PlatformIconGroup.actionsSuspend();
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    Presentation presentation = e.getPresentation();
    if (project == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }

    JBIterable<RunDashboardRunConfigurationNode> targetNodes = getLeafTargets(e);
    boolean enabled = targetNodes.filter(node -> {
      Content content = node.getContent();
      return content != null && !RunContentManagerImpl.isTerminated(content);
    }).isNotEmpty();
    presentation.setEnabled(enabled);
    presentation.setVisible(enabled || !ActionPlaces.isPopupPlace(e.getPlace()));
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) return;

    for (RunDashboardRunConfigurationNode node : getLeafTargets(e)) {
      ExecutionManagerImpl.stopProcess(node.getDescriptor());
    }
  }
}
