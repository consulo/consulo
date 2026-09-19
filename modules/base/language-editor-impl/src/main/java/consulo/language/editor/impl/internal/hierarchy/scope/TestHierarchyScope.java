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
package consulo.language.editor.impl.internal.hierarchy.scope;

import consulo.language.editor.hierarchy.HierarchyScope;

import consulo.content.scope.SearchScope;
import consulo.language.psi.scope.GlobalSearchScopesCore;
import consulo.language.editor.localize.LanguageEditorLocalize;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.content.TestSourcesFilter;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author UNV
 * @since 2025-04-24
 */
public class TestHierarchyScope extends HierarchyScopeBase {
    public static final TestHierarchyScope INSTANCE = new TestHierarchyScope();

    
    @Override
    public LocalizeValue getPresentableName() {
        return LanguageEditorLocalize.hierarchyScopeTest();
    }

    @Override
    public boolean isInScope(PsiElement baseClass, PsiElement srcElement) {
        VirtualFile virtualFile = srcElement.getContainingFile().getVirtualFile();
        return virtualFile == null || TestSourcesFilter.isTestSources(virtualFile, srcElement.getProject());
    }

    
    @Override
    public SearchScope getSearchScope(PsiElement thisClass) {
        return GlobalSearchScopesCore.projectTestScope(thisClass.getProject());
    }

    private TestHierarchyScope() {
    }
}
