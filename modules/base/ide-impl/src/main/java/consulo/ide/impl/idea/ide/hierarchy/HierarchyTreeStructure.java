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

package consulo.ide.impl.idea.ide.hierarchy;

import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.project.Project;
import consulo.project.content.TestSourcesFilter;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.ide.impl.psi.search.GlobalSearchScopes;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.content.scope.SearchScope;
import consulo.content.scope.NamedScope;
import consulo.language.editor.scope.NamedScopeManager;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSet;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.style.StandardColors;
import consulo.util.concurrent.ActionCallback;

import javax.annotation.Nonnull;

public abstract class HierarchyTreeStructure extends AbstractTreeStructure {
  protected HierarchyNodeDescriptor myBaseDescriptor;
  private HierarchyNodeDescriptor myRoot;
  protected final Project myProject;

  protected HierarchyTreeStructure(@Nonnull Project project, HierarchyNodeDescriptor baseDescriptor) {
    myBaseDescriptor = baseDescriptor;
    myProject = project;
    myRoot = baseDescriptor;
  }

  public final HierarchyNodeDescriptor getBaseDescriptor() {
    return myBaseDescriptor;
  }

  protected final void setBaseElement(@Nonnull HierarchyNodeDescriptor baseElement) {
    myBaseDescriptor = baseElement;
    myRoot = baseElement;
    while(myRoot.getParentDescriptor() != null){
      myRoot = (HierarchyNodeDescriptor)myRoot.getParentDescriptor();
    }
  }

  @Override
  @Nonnull
  public final NodeDescriptor createDescriptor(final Object element, final NodeDescriptor parentDescriptor) {
    if (element instanceof HierarchyNodeDescriptor) {
      return (HierarchyNodeDescriptor)element;
    }
    if (element instanceof String) {
      return new TextInfoNodeDescriptor(parentDescriptor, (String)element, myProject);
    }
    throw new IllegalArgumentException("Unknown element type: " + element);
  }

  @Override
  public final boolean isToBuildChildrenInBackground(final Object element) {
    if (element instanceof HierarchyNodeDescriptor){
      final HierarchyNodeDescriptor descriptor = (HierarchyNodeDescriptor)element;
      final Object[] cachedChildren = descriptor.getCachedChildren();
      if (cachedChildren == null && descriptor.isValid()){
        return true;
      }
    }
    return false;
  }

  @Override
  public final Object[] getChildElements(final Object element) {
    if (element instanceof HierarchyNodeDescriptor) {
      final HierarchyNodeDescriptor descriptor = (HierarchyNodeDescriptor)element;
      final Object[] cachedChildren = descriptor.getCachedChildren();
      if (cachedChildren == null) {
        if (!descriptor.isValid()){ //invalid
          descriptor.setCachedChildren(ArrayUtil.EMPTY_OBJECT_ARRAY);
        }
        else{
          descriptor.setCachedChildren(buildChildren(descriptor));
        }
      }
      return descriptor.getCachedChildren();
    }
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public final Object getParentElement(final Object element) {
    if (element instanceof HierarchyNodeDescriptor) {
      return ((HierarchyNodeDescriptor)element).getParentDescriptor();
    }

    return null;
  }

  @Override
  public final void commit() {
    PsiDocumentManager.getInstance(myProject).commitAllDocuments();
  }

  @Override
  public final boolean hasSomethingToCommit() {
    return PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments();
  }
  @Nonnull
  @Override
  public ActionCallback asyncCommit() {
    return PsiDocumentManager.asyncCommitDocuments(myProject);
  }

  protected abstract Object[] buildChildren(HierarchyNodeDescriptor descriptor);

  @Override
  public final Object getRootElement() {
    return myRoot;
  }

  protected SearchScope getSearchScope(final String scopeType, final PsiElement thisClass) {
    SearchScope searchScope = GlobalSearchScope.allScope(myProject);
    if (HierarchyBrowserBaseEx.SCOPE_CLASS.equals(scopeType)) {
      searchScope = new LocalSearchScope(thisClass);
    }
    else if (HierarchyBrowserBaseEx.SCOPE_PROJECT.equals(scopeType)) {
      searchScope = GlobalSearchScopes.projectProductionScope(myProject);
    }
    else if (HierarchyBrowserBaseEx.SCOPE_TEST.equals(scopeType)) {
      searchScope = GlobalSearchScopes.projectTestScope(myProject);
    } else {
      final NamedScope namedScope = NamedScopesHolder.getScope(myProject, scopeType);
      if (namedScope != null) {
        searchScope = GlobalSearchScopes.filterScope(myProject, namedScope);
      }
    }
    return searchScope;
  }

  protected boolean isInScope(final PsiElement baseClass, final PsiElement srcElement, final String scopeType) {
    if (HierarchyBrowserBaseEx.SCOPE_CLASS.equals(scopeType)) {
      if (!PsiTreeUtil.isAncestor(baseClass, srcElement, true)) {
        return false;
      }
    }
    else if (HierarchyBrowserBaseEx.SCOPE_PROJECT.equals(scopeType)) {
      final VirtualFile virtualFile = srcElement.getContainingFile().getVirtualFile();
      if (virtualFile != null && TestSourcesFilter.isTestSources(virtualFile, myProject)) {
        return false;
      }
    }
    else if (HierarchyBrowserBaseEx.SCOPE_TEST.equals(scopeType)) {

      final VirtualFile virtualFile = srcElement.getContainingFile().getVirtualFile();
      if (virtualFile != null && !TestSourcesFilter.isTestSources(virtualFile, myProject)) {
        return false;
      }
    } else if (!HierarchyBrowserBaseEx.SCOPE_ALL.equals(scopeType)) {
      final NamedScope namedScope = NamedScopesHolder.getScope(myProject, scopeType);
      if (namedScope == null) {
        return false;
      }
      final PackageSet namedScopePattern = namedScope.getValue();
      if (namedScopePattern == null) {
        return false;
      }
      final PsiFile psiFile = srcElement.getContainingFile();
      if (psiFile != null && !namedScopePattern.contains(psiFile.getVirtualFile(), psiFile.getProject(), NamedScopesHolder.getHolder(myProject, scopeType, NamedScopeManager.getInstance(myProject)))) {
        return false;
      }
    }
    return true;
  }

  private static final class TextInfoNodeDescriptor extends NodeDescriptor {
    public TextInfoNodeDescriptor(final NodeDescriptor parentDescriptor, final String text, final Project project) {
      super(parentDescriptor);
      myName = text;
      myColor = StandardColors.RED;
    }

    @Override
    public final Object getElement() {
      return myName;
    }

    @RequiredUIAccess
    @Override
    public final boolean update() {
      return true;
    }
  }

  public boolean isAlwaysShowPlus() {
    return false;
  }
}
