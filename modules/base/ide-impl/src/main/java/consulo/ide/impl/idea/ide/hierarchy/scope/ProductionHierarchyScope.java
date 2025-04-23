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

import consulo.content.scope.SearchScope;
import consulo.ide.impl.psi.search.GlobalSearchScopes;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.content.TestSourcesFilter;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author UNV
 * @since 2025-04-24
 */
public class ProductionHierarchyScope extends HierarchyScopeBase {
    public static final ProductionHierarchyScope INSTANCE = new ProductionHierarchyScope();

    @Nonnull
    @Override
    public LocalizeValue getPresentableName() {
        return IdeLocalize.hierarchyScopeProject();
    }

    @Override
    public boolean isInScope(PsiElement baseClass, @Nonnull PsiElement srcElement) {
        VirtualFile virtualFile = srcElement.getContainingFile().getVirtualFile();
        return virtualFile == null || !TestSourcesFilter.isTestSources(virtualFile, srcElement.getProject());
    }

    @Nonnull
    @Override
    public SearchScope getSearchScope(PsiElement thisClass) {
        return GlobalSearchScopes.projectProductionScope(thisClass.getProject());
    }

    private ProductionHierarchyScope() {
    }
}
