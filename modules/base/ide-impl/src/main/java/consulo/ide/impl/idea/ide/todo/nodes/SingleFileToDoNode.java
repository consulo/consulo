/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
import consulo.ide.impl.idea.ide.todo.TodoTreeBuilder;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.language.psi.search.TodoItem;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class SingleFileToDoNode extends BaseToDoNode<PsiFile> {
  private final TodoFileNode myFileNode;

  public SingleFileToDoNode(Project project, @Nonnull PsiFile value, TodoTreeBuilder builder) {
    super(project, value, builder);
    myFileNode = new TodoFileNode(getProject(), value, myBuilder, true);
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public Collection<AbstractTreeNode> getChildren() {
    return new ArrayList<>(Collections.singleton(myFileNode));
  }

  @Override
  public void update(@Nonnull PresentationData presentation) {
  }

  @Override
  public boolean canRepresent(Object element) {
    return false;
  }

  @Override
  public boolean contains(Object element) {
    if (element instanceof TodoItem) {
      return super.canRepresent(((TodoItem)element).getFile());
    }
    return super.canRepresent(element);
  }

  public Object getFileNode() {
    return myFileNode;
  }

  @Override
  public int getFileCount(PsiFile val) {
    return 1;
  }

  @Override
  public int getTodoItemCount(PsiFile val) {
    return getTreeStructure().getTodoItemCount(val);
  }
}
