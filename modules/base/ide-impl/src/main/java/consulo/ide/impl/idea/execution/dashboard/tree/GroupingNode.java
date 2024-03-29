/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.execution.dashboard.tree;

import consulo.ide.impl.idea.execution.dashboard.DashboardGroup;
import consulo.ide.impl.idea.execution.dashboard.DashboardNode;
import consulo.annotation.access.RequiredReadAction;
import consulo.ui.ex.tree.PresentationData;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.Project;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author konstantin.aleev
 */
public class GroupingNode extends AbstractTreeNode<Pair<Object, DashboardGroup>> implements DashboardNode {
  private final List<AbstractTreeNode> myChildren = new ArrayList<>();

  public GroupingNode(Project project, Object parent, DashboardGroup group) {
    super(project, Pair.create(parent, group));
  }

  public DashboardGroup getGroup() {
    //noinspection ConstantConditions ???
    return getValue().getSecond();
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public Collection<? extends AbstractTreeNode> getChildren() {
    return myChildren;
  }

  public void addChildren(Collection<? extends AbstractTreeNode> children) {
    myChildren.addAll(children);
  }

  @Override
  protected void update(PresentationData presentation) {
    presentation.setPresentableText(getGroup().getName());
    presentation.setIcon(getGroup().getIcon());
  }
}
