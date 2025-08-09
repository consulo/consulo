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

package consulo.ide.impl.idea.ide.todo;

import consulo.language.editor.todo.TodoFilter;
import consulo.project.ui.view.tree.TreeStructureProvider;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.AbstractTreeStructureBase;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.project.Project;
import consulo.util.concurrent.ActionCallback;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.search.PsiTodoSearchHelper;
import consulo.language.psi.search.TodoPattern;
import consulo.language.psi.PsiPackageSupportProviders;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Vladimir Kondratyev
 */
public abstract class TodoTreeStructure extends AbstractTreeStructureBase implements ToDoSettings {
  protected TodoTreeBuilder myBuilder;
  protected AbstractTreeNode myRootElement;
  protected final ToDoSummary mySummaryElement;

  private boolean myFlattenPackages;
  protected boolean myArePackagesShown;
  private boolean myAreModulesShown;


  protected final PsiTodoSearchHelper mySearchHelper;
  /**
   * Current {@code TodoFilter}. If no filter is set then this field is {@code null}.
   */
  protected TodoFilter myTodoFilter;

  public TodoTreeStructure(Project project) {
    super(project);
    myArePackagesShown = PsiPackageSupportProviders.isPackageSupported(project);
    mySummaryElement = new ToDoSummary();
    mySearchHelper = PsiTodoSearchHelper.getInstance(project);
  }

  final void setTreeBuilder(TodoTreeBuilder builder) {
    myBuilder = builder;
    myRootElement = createRootElement();
  }

  protected abstract AbstractTreeNode createRootElement();

  public abstract boolean accept(PsiFile psiFile);

  /**
   * Validate whole the cache
   */
  protected void validateCache() {
  }

  public final boolean isPackagesShown() {
    return myArePackagesShown;
  }

  final void setShownPackages(boolean state) {
    myArePackagesShown = state;
  }

  public final boolean areFlattenPackages() {
    return myFlattenPackages;
  }

  public final void setFlattenPackages(boolean state) {
    myFlattenPackages = state;
  }

  /**
   * Sets new {@code TodoFilter}. {@code null} is acceptable value. It means
   * that there is no any filtration of <code>TodoItem>/code>s.
   */
  final void setTodoFilter(TodoFilter todoFilter) {
    myTodoFilter = todoFilter;
  }

  /**
   * @return first element that can be selected in the tree. The method can returns {@code null}.
   */
  abstract Object getFirstSelectableElement();

  /**
   * @return number of {@code TodoItem}s located in the file.
   */
  public final int getTodoItemCount(PsiFile psiFile) {
    int count = 0;
    if (psiFile != null) {
      if (myTodoFilter != null) {
        for (Iterator i = myTodoFilter.iterator(); i.hasNext(); ) {
          TodoPattern pattern = (TodoPattern)i.next();
          count += getSearchHelper().getTodoItemsCount(psiFile, pattern);
        }
      }
      else {
        count = getSearchHelper().getTodoItemsCount(psiFile);
      }
    }
    return count;
  }

  boolean isAutoExpandNode(NodeDescriptor descriptor) {
    Object element = descriptor.getElement();
    if (element instanceof AbstractTreeNode) {
      element = ((AbstractTreeNode)element).getValue();
    }
    return element == getRootElement() || element == mySummaryElement && (myAreModulesShown || myArePackagesShown);
  }

  @Override
  public final void commit() {
    PsiDocumentManager.getInstance(myProject).commitAllDocuments();
  }

  @Override
  public boolean hasSomethingToCommit() {
    return PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments();
  }

  @Nonnull
  @Override
  public ActionCallback asyncCommit() {
    return PsiDocumentManager.asyncCommitDocuments(myProject);
  }

  @Nonnull
  @Override
  public final Object getRootElement() {
    return myRootElement;
  }

  public boolean getIsFlattenPackages() {
    return myFlattenPackages;
  }

  public PsiTodoSearchHelper getSearchHelper() {
    return mySearchHelper;
  }

  public TodoFilter getTodoFilter() {
    return myTodoFilter;
  }

  @Override
  public List<TreeStructureProvider> getProviders() {
    return Collections.emptyList();
  }

  void setShownModules(boolean state) {
    myAreModulesShown = state;
  }

  @Override
  public boolean isModulesShown() {
    return myAreModulesShown;
  }
}