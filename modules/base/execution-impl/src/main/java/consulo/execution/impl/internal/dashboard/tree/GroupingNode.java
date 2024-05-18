// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard.tree;

import consulo.execution.dashboard.RunDashboardGroup;
import consulo.execution.dashboard.RunDashboardNode;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author konstantin.aleev
 */
public final class GroupingNode extends AbstractTreeNode<Pair<Object, RunDashboardGroup>> implements RunDashboardNode {
  private final List<AbstractTreeNode<?>> myChildren = new ArrayList<>();

  public GroupingNode(Project project, Object parent, RunDashboardGroup group) {
    super(project, Pair.create(parent, group));
  }

  public RunDashboardGroup getGroup() {
    //noinspection ConstantConditions ???
    return getValue().getSecond();
  }

  @Nonnull
  @Override
  public Collection<? extends AbstractTreeNode<?>> getChildren() {
    return myChildren;
  }

  public void addChildren(Collection<? extends AbstractTreeNode<?>> children) {
    myChildren.addAll(children);
  }

  @Override
  protected void update(@Nonnull PresentationData presentation) {
    presentation.setPresentableText(getGroup().getName());
    presentation.setIcon(getGroup().getIcon());
  }
}
