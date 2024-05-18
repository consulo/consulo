// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard.tree;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.dashboard.RunDashboardGroup;
import consulo.execution.dashboard.RunDashboardGroupingRule;
import consulo.execution.dashboard.RunDashboardRunConfigurationNode;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.project.ui.view.tree.AbstractTreeNode;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nullable;

/**
 * @author konstantin.aleev
 */
@ExtensionImpl
public final class ConfigurationTypeDashboardGroupingRule implements RunDashboardGroupingRule {
  @NonNls
  public static final String NAME = "ConfigurationTypeDashboardGroupingRule";

  @Override
  @Nonnull
  public String getName() {
    return NAME;
  }

  @Nullable
  @Override
  public RunDashboardGroup getGroup(AbstractTreeNode<?> node) {
    Project project = node.getProject();
    if (project != null && !ProjectPropertiesComponent.getInstance(project).getBoolean(getName(), true)) {
      return null;
    }
    if (node instanceof RunDashboardRunConfigurationNode) {
      RunnerAndConfigurationSettings configurationSettings = ((RunDashboardRunConfigurationNode)node).getConfigurationSettings();
      ConfigurationType type = configurationSettings.getType();
      return new RunDashboardGroupImpl<>(type, type.getDisplayName().get(), type.getIcon());
    }
    return null;
  }
}
