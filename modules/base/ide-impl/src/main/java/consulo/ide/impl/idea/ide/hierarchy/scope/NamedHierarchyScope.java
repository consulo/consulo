/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.idea.ide.hierarchy.scope;

import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSet;
import consulo.content.scope.SearchScope;
import consulo.ide.impl.psi.search.GlobalSearchScopes;
import consulo.language.editor.scope.NamedScopeManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

/**
 * @author UNV
 * @since 2025-04-24
 */
public class NamedHierarchyScope implements HierarchyScope {
    @Nonnull
    private final NamedScope myNamedScope;

    public NamedHierarchyScope(@Nonnull NamedScope namedScope) {
        myNamedScope = namedScope;
    }

    @Nonnull
    @Override
    public String getId() {
        return myNamedScope.getScopeId();
    }

    @Nonnull
    @Override
    public LocalizeValue getPresentableName() {
        return myNamedScope.getPresentableName();
    }

    @Override
    public boolean isInScope(PsiElement baseClass, @Nonnull PsiElement srcElement) {
        PackageSet namedScopePattern = myNamedScope.getValue();
        if (namedScopePattern == null) {
            return false;
        }
        PsiFile psiFile = srcElement.getContainingFile();
        Project project = srcElement.getProject();
        return psiFile == null || namedScopePattern.contains(
            psiFile.getVirtualFile(),
            psiFile.getProject(),
            NamedScopesHolder.getHolder(project, myNamedScope.getScopeId(), NamedScopeManager.getInstance(project))
        );
    }

    @Nonnull
    @Override
    public SearchScope getSearchScope(PsiElement thisClass) {
        return GlobalSearchScopes.filterScope(thisClass.getProject(), myNamedScope);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
            || obj instanceof NamedHierarchyScope that
            && myNamedScope.getScopeId().equals(that.myNamedScope.getScopeId());
    }

    @Override
    public int hashCode() {
        return myNamedScope.getScopeId().hashCode();
    }

    public static HierarchyScope find(@Nonnull Project project, @Nonnull String scopeId) {
        NamedScope namedScope = NamedScopesHolder.getScope(project, scopeId);
        return namedScope != null ? new NamedHierarchyScope(namedScope) : null;
    }
}
