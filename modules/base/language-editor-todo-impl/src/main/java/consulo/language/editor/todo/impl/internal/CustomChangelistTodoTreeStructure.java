/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.todo.impl.internal;

import consulo.language.editor.todo.impl.internal.node.ToDoRootNode;
import consulo.language.psi.PsiFile;
import consulo.language.psi.search.PsiTodoSearchHelper;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;

/**
 * @author irengrig
 * @since 2011-02-21
 */
public class CustomChangelistTodoTreeStructure extends TodoTreeStructure {
  private final PsiTodoSearchHelper mySearchHelper;

  public CustomChangelistTodoTreeStructure(Project project, PsiTodoSearchHelper searchHelper) {
    super(project);
    mySearchHelper = searchHelper;
  }

  @Override
  public boolean accept(PsiFile psiFile) {
    if (! psiFile.isValid()) return false;
    return mySearchHelper.getTodoItemsCount(psiFile) > 0;
  }

  @Override
  public boolean getIsPackagesShown() {
    return myArePackagesShown;
  }

  @Override
  Object getFirstSelectableElement() {
    return ((ToDoRootNode)myRootElement).getSummaryNode();
  }

  @Override
  protected AbstractTreeNode createRootElement() {
    return new ToDoRootNode(myProject, new Object(), myBuilder, mySummaryElement);
  }

  @Override
  public PsiTodoSearchHelper getSearchHelper() {
    return mySearchHelper;
  }
}
