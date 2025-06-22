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
package consulo.ide.impl.idea.ide.todo;

import consulo.ide.impl.idea.ide.todo.nodes.ToDoRootNode;
import consulo.find.ui.ScopeChooserCombo;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.content.scope.SearchScope;

/**
 * @author anna
 * @since 2007-07-27
 */
public class ScopeBasedTodosTreeStructure extends TodoTreeStructure {
  private final ScopeChooserCombo myScopes;

  public ScopeBasedTodosTreeStructure(Project project, ScopeChooserCombo scopes) {
    super(project);
    myScopes = scopes;
  }

  @Override
  public boolean accept(final PsiFile psiFile) {
    if (!psiFile.isValid()) return false;

    SearchScope scope = myScopes.getSelectedScope();
    VirtualFile file = psiFile.getVirtualFile();
    boolean isAffected = scope != null && file != null && scope.contains(file);
    return isAffected && (myTodoFilter != null && myTodoFilter.accept(mySearchHelper, psiFile) ||
                          (myTodoFilter == null && mySearchHelper.getTodoItemsCount(psiFile) > 0));
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
}