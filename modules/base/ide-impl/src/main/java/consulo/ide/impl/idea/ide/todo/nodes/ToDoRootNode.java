/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.todo.nodes;

import consulo.annotation.access.RequiredReadAction;
import consulo.ui.ex.tree.PresentationData;
import consulo.ide.impl.idea.ide.todo.ToDoSummary;
import consulo.ide.impl.idea.ide.todo.TodoTreeBuilder;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ToDoRootNode extends BaseToDoNode {
  private final SummaryNode mySummaryNode;

  public ToDoRootNode(Project project, Object value, TodoTreeBuilder builder, @Nonnull ToDoSummary summary) {
    super(project, value, builder);
    mySummaryNode = createSummaryNode(summary);
  }

  protected SummaryNode createSummaryNode(@Nonnull ToDoSummary summary) {
    return new SummaryNode(getProject(), summary, myBuilder);
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public Collection<AbstractTreeNode> getChildren() {
    return new ArrayList<>(Collections.singleton(mySummaryNode));
  }

  @Override
  public void update(@Nonnull PresentationData presentation) {
  }

  public Object getSummaryNode() {
    return mySummaryNode;
  }

  @Override
  public String getTestPresentation() {
    return "Root";
  }

  @Override
  public int getFileCount(Object val) {
    return mySummaryNode.getFileCount(null);
  }

  @Override
  public int getTodoItemCount(Object val) {
    return mySummaryNode.getTodoItemCount(null);
  }
}
