// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.http;

import consulo.annotation.component.ExtensionImpl;
import consulo.endpoint.http.HttpHeaderDictionary;
import consulo.endpoint.http.HttpHeaderDocumentation;
import consulo.language.editor.documentation.UnrestrictedDocumentationProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class HttpHeaderReferenceDocumentationProvider implements UnrestrictedDocumentationProvider {
    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        HttpHeaderDocumentation documentation = getDocumentation(element);
        return documentation != null ? documentation.generateDoc() : null;
    }

    private static @Nullable HttpHeaderDocumentation getDocumentation(PsiElement element) {
        if (element instanceof HttpHeaderElement headerElement) {
            String name = headerElement.getName();
            if (StringUtil.isNotEmpty(name)) {
                return HttpHeaderDictionary.getDocumentation(name);
            }
        }
        return null;
    }

    @Override
    public @Nullable PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object item, @Nullable PsiElement element) {
        if (element != null && item instanceof HttpHeaderDocumentation documentation) {
            return new HttpHeaderElement(element, documentation.getName());
        }
        return null;
    }
}
