// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard.tree;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.dashboard.RunDashboardGroup;
import consulo.execution.dashboard.RunDashboardGroupingRule;
import consulo.execution.dashboard.RunDashboardRunConfigurationNode;
import consulo.execution.dashboard.RunDashboardRunConfigurationStatus;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.project.ui.view.tree.AbstractTreeNode;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;

/**
 * @author konstantin.aleev
 */
@ExtensionImpl
public final class StatusDashboardGroupingRule implements RunDashboardGroupingRule {
  @NonNls
  public static final String NAME = "StatusDashboardGroupingRule";

  @Override
  @Nonnull
  public String getName() {
    return NAME;
  }

  @Nullable
  @Override
  public RunDashboardGroup getGroup(AbstractTreeNode<?> node) {
    Project project = node.getProject();
    if (project != null && !ProjectPropertiesComponent.getInstance(project).getBoolean(getName(), false)) {
      return null;
    }
    if (node instanceof RunDashboardRunConfigurationNode runConfigurationNode) {
      RunDashboardRunConfigurationStatus status = runConfigurationNode.getStatus();
      return new RunDashboardGroupImpl<>(status, status.getName(), status.getIcon());
    }
    return null;
  }
}
