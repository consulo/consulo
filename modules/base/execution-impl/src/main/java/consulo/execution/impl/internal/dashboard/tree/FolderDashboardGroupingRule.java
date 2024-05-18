// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard.tree;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.dashboard.RunDashboardGroup;
import consulo.execution.dashboard.RunDashboardGroupingRule;
import consulo.execution.dashboard.RunDashboardRunConfigurationNode;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author konstantin.aleev
 */
@ExtensionImpl(id = "folder", order = "after status")
public final class FolderDashboardGroupingRule implements RunDashboardGroupingRule {
  private static final String NAME = "FolderDashboardGroupingRule";

  @Override
  @Nonnull
  public String getName() {
    return NAME;
  }

  @Nullable
  @Override
  public RunDashboardGroup getGroup(AbstractTreeNode<?> node) {
    if (node instanceof RunDashboardRunConfigurationNode) {
      RunnerAndConfigurationSettings configurationSettings = ((RunDashboardRunConfigurationNode)node).getConfigurationSettings();
      String folderName = configurationSettings.getFolderName();
      if (folderName != null) {
        return new FolderDashboardGroup(node.getProject(), folderName, folderName, PlatformIconGroup.nodesFolder());
      }
    }
    return null;
  }

  public static final class FolderDashboardGroup extends RunDashboardGroupImpl<String> {
    private final Project myProject;

    public FolderDashboardGroup(Project project, String value, String name, Image icon) {
      super(value, name, icon);
      myProject = project;
    }

    public Project getProject() {
      return myProject;
    }
  }
}
