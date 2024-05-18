// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.impl.internal.dashboard.action;

import consulo.execution.dashboard.RunDashboardRunConfigurationNode;
import consulo.execution.impl.internal.dashboard.RunDashboardServiceViewContributor;
import consulo.execution.impl.internal.dashboard.tree.GroupingNode;
import consulo.execution.impl.internal.service.ServiceViewManagerImpl;
import consulo.execution.service.ServiceViewActionUtils;
import consulo.execution.service.ServiceViewManager;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.JBIterable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

final class RunDashboardActionUtils {
  private RunDashboardActionUtils() {
  }

  @Nonnull
  static List<RunDashboardRunConfigurationNode> getTargets(@Nonnull AnActionEvent e) {
    return ServiceViewActionUtils.getTargets(e, RunDashboardRunConfigurationNode.class);
  }

  @Nullable
  static RunDashboardRunConfigurationNode getTarget(@Nonnull AnActionEvent e) {
    return ServiceViewActionUtils.getTarget(e, RunDashboardRunConfigurationNode.class);
  }

  @Nonnull
  static JBIterable<RunDashboardRunConfigurationNode> getLeafTargets(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) return JBIterable.empty();

    JBIterable<Object> roots = JBIterable.of(e.getData(PlatformDataKeys.SELECTED_ITEMS));
    Set<RunDashboardRunConfigurationNode> result = new LinkedHashSet<>();
    if (!getLeaves(project, e, roots.toList(), Collections.emptyList(), result)) return JBIterable.empty();

    return JBIterable.from(result);
  }

  private static boolean getLeaves(Project project, AnActionEvent e, List<Object> items, List<Object> valueSubPath,
                                   Set<? super RunDashboardRunConfigurationNode> result) {
    for (Object item : items) {
      if (item instanceof RunDashboardServiceViewContributor || item instanceof GroupingNode) {
        List<Object> itemSubPath = new ArrayList<>(valueSubPath);
        itemSubPath.add(item);
        List<Object> children = ((ServiceViewManagerImpl)ServiceViewManager.getInstance(project))
          .getChildrenSafe(e, itemSubPath, RunDashboardServiceViewContributor.class);
        if (!getLeaves(project, e, children, itemSubPath, result)) {
          return false;
        }
      }
      else if (item instanceof RunDashboardRunConfigurationNode) {
        result.add((RunDashboardRunConfigurationNode)item);
      }
      else if (item instanceof AbstractTreeNode) {
        AbstractTreeNode<?> parent = (AbstractTreeNode<?>)((AbstractTreeNode<?>)item).getParent();
        if (parent instanceof RunDashboardRunConfigurationNode) {
          result.add((RunDashboardRunConfigurationNode)parent);
        }
        else {
          return false;
        }
      }
      else {
        return false;
      }
    }
    return true;
  }
}
