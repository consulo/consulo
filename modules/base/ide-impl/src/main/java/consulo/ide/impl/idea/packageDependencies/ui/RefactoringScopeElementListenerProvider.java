/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.packageDependencies.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.scope.*;
import consulo.ide.impl.psi.search.scope.packageSet.IntersectionPackageSet;
import consulo.content.scope.PatternBasedPackageSet;
import consulo.ide.impl.psi.search.scope.packageSet.UnionPackageSet;
import consulo.language.editor.scope.NamedScopeManager;
import consulo.logging.Logger;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiQualifiedNamedElement;
import consulo.language.editor.refactoring.event.RefactoringElementAdapter;
import consulo.language.editor.refactoring.event.RefactoringElementListener;
import consulo.language.editor.refactoring.event.RefactoringElementListenerComposite;
import consulo.language.editor.refactoring.event.RefactoringElementListenerProvider;
import jakarta.annotation.Nonnull;

/**
 * @author anna
 * @since 2010-07-26
 */
@ExtensionImpl
public class RefactoringScopeElementListenerProvider implements RefactoringElementListenerProvider {
  private static final Logger LOG = Logger.getInstance(RefactoringScopeElementListenerProvider.class);

  @Override
  public RefactoringElementListener getListener(PsiElement element) {
    PsiFile containingFile = element.getContainingFile();
    if (!(element instanceof PsiQualifiedNamedElement)) return null;
    String oldName = ((PsiQualifiedNamedElement)element).getQualifiedName();
    RefactoringElementListenerComposite composite = null;
    for (NamedScopesHolder holder : NamedScopeManager.getAllNamedScopeHolders(element.getProject())) {
      NamedScope[] scopes = holder.getEditableScopes();
      for (int i = 0; i < scopes.length; i++) {
        NamedScope scope = scopes[i];
        PackageSet packageSet = scope.getValue();
        if (packageSet != null && (containingFile == null || packageSet.contains(containingFile.getVirtualFile(), containingFile.getProject(), holder))) {
          composite = traverse(new OldScopeDescriptor(oldName, scope, i, holder), composite, packageSet);
        }
      }
    }
    return composite;
  }

  private static RefactoringElementListenerComposite traverse(OldScopeDescriptor scopeDescriptor,
                                                              RefactoringElementListenerComposite composite,
                                                              PackageSet packageSet) {
    if (packageSet instanceof PatternBasedPackageSet) {
      composite = checkPatternPackageSet(scopeDescriptor, composite, ((PatternBasedPackageSet)packageSet),
                                         scopeDescriptor.getScope().getValue().getText());
    }
    else if (packageSet instanceof ComplementPackageSet) {
      composite = traverse(scopeDescriptor, composite, ((ComplementPackageSet)packageSet).getComplementarySet());
    }
    else if (packageSet instanceof UnionPackageSet) {
      composite = traverse(scopeDescriptor, composite, ((UnionPackageSet)packageSet).getFirstSet());
      composite = traverse(scopeDescriptor, composite, ((UnionPackageSet)packageSet).getSecondSet());
    }
    else if (packageSet instanceof IntersectionPackageSet) {
      composite = traverse(scopeDescriptor, composite, ((IntersectionPackageSet)packageSet).getFirstSet());
      composite = traverse(scopeDescriptor, composite, ((IntersectionPackageSet)packageSet).getSecondSet());
    }
    return composite;
  }

  private static RefactoringElementListenerComposite checkPatternPackageSet(final OldScopeDescriptor descriptor,
                                                                            RefactoringElementListenerComposite composite,
                                                                            PatternBasedPackageSet pattern,
                                                                            final String text) {
    if (pattern.isOn(descriptor.getOldQName())) {
      if (composite == null) {
        composite = new RefactoringElementListenerComposite();
      }
      composite.addListener(new RefactoringElementAdapter() {
        @Override
        public void elementRenamedOrMoved(@Nonnull PsiElement newElement) {
          LOG.assertTrue(newElement instanceof PsiQualifiedNamedElement);
          try {
            String newPattern = text.replace(descriptor.getOldQName(), ((PsiQualifiedNamedElement)newElement).getQualifiedName());
            PackageSet newSet = PackageSetFactory.getInstance().compile(newPattern);
            NamedScope newScope = new NamedScope(descriptor.getScope().getName(), newSet);
            NamedScope[] currentScopes = descriptor.getHolder().getEditableScopes();
            currentScopes[descriptor.getIdx()] = newScope;
            descriptor.getHolder().setScopes(currentScopes);
          }
          catch (ParsingException ignore) {
          }
        }

        @Override
        public void undoElementMovedOrRenamed(@Nonnull PsiElement newElement, @Nonnull String oldQualifiedName) {
          LOG.assertTrue(newElement instanceof PsiQualifiedNamedElement);
          try {
            NamedScope[] currentScopes = descriptor.getHolder().getEditableScopes();
            String oldPattern = ((PatternBasedPackageSet)currentScopes[descriptor.getIdx()].getValue()).getPattern()
              .replace(((PsiQualifiedNamedElement)newElement).getQualifiedName(), oldQualifiedName);
            PackageSet newSet = PackageSetFactory.getInstance().compile(oldPattern);
            NamedScope newScope = new NamedScope(descriptor.getScope().getName(), newSet);
            currentScopes[descriptor.getIdx()] = newScope;
            descriptor.getHolder().setScopes(currentScopes);
          }
          catch (ParsingException ignore) {
          }
        }
      });
    }
    return composite;
  }

  private static class OldScopeDescriptor {
    private final String myOldQName;
    private final NamedScopesHolder myHolder;
    private final int myIdx;
    private final NamedScope myScope;

    private OldScopeDescriptor(String oldQName,
                               NamedScope scope,
                               int idx,
                               NamedScopesHolder holder) {
      myOldQName = oldQName;
      myHolder = holder;
      myIdx = idx;
      myScope = scope;
    }

    public String getOldQName() {
      return myOldQName;
    }

    public NamedScopesHolder getHolder() {
      return myHolder;
    }

    public int getIdx() {
      return myIdx;
    }

    public NamedScope getScope() {
      return myScope;
    }
  }
}
