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
package consulo.language.editor.todo.impl.internal;

import consulo.language.editor.todo.impl.internal.node.ToDoRootNode;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import java.util.Collection;

/**
 * @author anna
 * @since 2007-07-27
 */
public class ChangeListTodosTreeStructure extends TodoTreeStructure {
  public ChangeListTodosTreeStructure(Project project) {
    super(project);
  }

  @Override
  public boolean accept(PsiFile psiFile) {
    if (!psiFile.isValid()) return false;
    boolean isAffected = false;
    Collection<Change> changes = ChangeListManager.getInstance(myProject).getDefaultChangeList().getChanges();
    for (Change change : changes) {
      if (change.affectsFile(VirtualFileUtil.virtualToIoFile(psiFile.getVirtualFile()))) {
        isAffected = true;
        break;
      }
    }
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