// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.inlay;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.Language;
import consulo.language.psi.PsiElement;

import java.util.Collection;
import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface UrlPathInlayLanguagesProvider {
    Collection<Language> getLanguages();

    default List<PsiElement> getPotentialElementsWithHintsProviders(PsiElement element) {
        return List.of();
    }
}
