/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.bookmark.ui.view;

import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.annotation.access.RequiredReadAction;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Irina.Chernushina
 * @since 2012-06-07
 */
public abstract class ProjectViewNodeWithChildrenList<T> extends ProjectViewNode<T> {
  protected final List<AbstractTreeNode> myChildren;

  protected ProjectViewNodeWithChildrenList(Project project, T t, ViewSettings viewSettings) {
    super(project, t, viewSettings);
    myChildren = new ArrayList<>();
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public Collection<? extends AbstractTreeNode> getChildren() {
    return myChildren;
  }

  public void addChild(AbstractTreeNode node) {
    myChildren.add(node);
    node.setParent(this);
  }

  public void addChildBefore(AbstractTreeNode newNode, AbstractTreeNode existingNode) {
    int idx = -1;
    for (int i = 0; i < myChildren.size(); i++) {
      AbstractTreeNode node = myChildren.get(i);
      // exactly the same node!
      if (node == existingNode) {
        idx = i;
        break;
      }
    }
    if (idx == -1) {
      addChild(newNode);
    }
    else {
      myChildren.add(idx, newNode);
    }
  }
}
