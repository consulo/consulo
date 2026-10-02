// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.reference;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.PsiReferenceBase;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class SchemeReference extends PsiReferenceBase.Immediate<PsiElement> implements UrlSegmentReference {
    private final @Nullable String myGivenValue;
    private final List<String> mySupportedSchemes;

    public SchemeReference(@Nullable String givenValue, List<String> supportedSchemes, PsiLanguageInjectionHost host, TextRange range) {
        super(host, range, false, host);
        myGivenValue = givenValue;
        mySupportedSchemes = supportedSchemes;
    }

    public @Nullable String getGivenValue() {
        return myGivenValue;
    }

    public List<String> getSupportedSchemes() {
        return mySupportedSchemes;
    }
}
