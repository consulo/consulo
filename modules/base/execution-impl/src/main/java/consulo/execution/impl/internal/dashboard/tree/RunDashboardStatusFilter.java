// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard.tree;

import consulo.execution.dashboard.RunDashboardRunConfigurationNode;
import consulo.execution.dashboard.RunDashboardRunConfigurationStatus;
import consulo.project.ui.view.tree.AbstractTreeNode;
import jakarta.annotation.Nonnull;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Konstantin Aleev
 */
public final class RunDashboardStatusFilter {
  private final Set<RunDashboardRunConfigurationStatus> myFilteredStatuses = new HashSet<>();

  public boolean isVisible(AbstractTreeNode<?> node) {
    return !(node instanceof RunDashboardRunConfigurationNode) || isVisible(((RunDashboardRunConfigurationNode)node).getStatus());
  }

  public boolean isVisible(@Nonnull RunDashboardRunConfigurationStatus status) {
    return !myFilteredStatuses.contains(status);
  }

  public void hide(@Nonnull RunDashboardRunConfigurationStatus status) {
    myFilteredStatuses.add(status);
  }

  public void show(@Nonnull RunDashboardRunConfigurationStatus status) {
    myFilteredStatuses.remove(status);
  }
}
