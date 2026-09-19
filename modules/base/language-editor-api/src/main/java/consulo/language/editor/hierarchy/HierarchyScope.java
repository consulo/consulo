/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.hierarchy;

import consulo.content.scope.SearchScope;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;

/**
 * The part of the project a hierarchy is built over. The platform owns which scopes exist, offers them and
 * remembers the choice; a model only reads the chosen one back off its {@link HierarchyRequest}.
 *
 * @author UNV
 * @since 2025-04-24
 */
public interface HierarchyScope {
    /**
     * Stable and never localized - this is what is persisted.
     */
    String getId();

    LocalizeValue getPresentableName();

    boolean isInScope(PsiElement baseElement, PsiElement srcElement);

    SearchScope getSearchScope(PsiElement element);
}
