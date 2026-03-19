// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.dashboard;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.ui.RunContentDescriptor;
import consulo.language.psi.PsiElement;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.awt.dnd.DnDEvent;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * Register this extension to customize Run Dashboard.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class RunDashboardCustomizer {
  public static final Key<Map<Object, Object>> NODE_LINKS = Key.create("RunDashboardNodeLink");

  public abstract boolean isApplicable(RunnerAndConfigurationSettings settings, @Nullable RunContentDescriptor descriptor);

  public boolean updatePresentation(PresentationData presentation, RunDashboardRunConfigurationNode node) {
    return false;
  }

  /**
   * Returns node's status. Subclasses may override this method to provide custom statuses.
   *
   * @param node dashboard node
   * @return node's status. Returned status is used for grouping nodes by status.
   */
  public @Nullable RunDashboardRunConfigurationStatus getStatus(RunDashboardRunConfigurationNode node) {
    return null;
  }

  public @Nullable PsiElement getPsiElement(RunDashboardRunConfigurationNode node) {
    return null;
  }

  public @Nullable Collection<? extends AbstractTreeNode<?>> getChildren(RunDashboardRunConfigurationNode node) {
    return null;
  }

  public boolean canDrop(RunDashboardRunConfigurationNode node, DnDEvent event) {
    return false;
  }

  public void drop(RunDashboardRunConfigurationNode node, DnDEvent event) {
  }
}
