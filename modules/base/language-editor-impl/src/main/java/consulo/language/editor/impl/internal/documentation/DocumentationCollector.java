// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation;

import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

public abstract class DocumentationCollector {
    private final PsiElement myElement;
    private final @Nullable String myRef;

    private volatile @Nullable DocumentationProvider myProvider;
    private @Nullable String myEffectiveUrl;

    protected DocumentationCollector(
        PsiElement element,
        @Nullable String effectiveUrl,
        @Nullable String ref,
        @Nullable DocumentationProvider provider
    ) {
        myElement = element;
        myRef = ref;
        myEffectiveUrl = effectiveUrl;
        myProvider = provider;
    }

    public PsiElement getElement() {
        return myElement;
    }

    public @Nullable String getRef() {
        return myRef;
    }

    public @Nullable DocumentationProvider getProvider() {
        return myProvider;
    }

    protected void setProvider(@Nullable DocumentationProvider provider) {
        myProvider = provider;
    }

    public @Nullable String getEffectiveUrl() {
        return myEffectiveUrl;
    }

    protected void setEffectiveUrl(@Nullable String effectiveUrl) {
        myEffectiveUrl = effectiveUrl;
    }

    public abstract @Nullable String getDocumentation() throws Exception;
}
