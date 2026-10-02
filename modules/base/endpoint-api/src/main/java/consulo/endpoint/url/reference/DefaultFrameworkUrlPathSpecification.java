// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.reference;

import consulo.endpoint.url.FrameworkUrlPathSpecification;
import consulo.endpoint.url.UrlConstants;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PartiallyKnownString;

public final class DefaultFrameworkUrlPathSpecification extends FrameworkUrlPathSpecification {
    public static final DefaultFrameworkUrlPathSpecification INSTANCE = new DefaultFrameworkUrlPathSpecification();

    private final UrlPksParser myParser;

    private DefaultFrameworkUrlPathSpecification() {
        UrlPksParser parser = new UrlPksParser();
        parser.setSplitEscaper(UrlExtractorUtil::springLikePropertySplitEscaper);
        parser.setShouldHaveScheme(false);
        parser.setParseQueryParameters(false);
        myParser = parser;
    }

    @Override
    public UrlPathContext getUrlPathContext(PsiElement declaration) {
        return UrlPathContext.supportingSchemes(UrlConstants.HTTP_METHODS).subContext(
            getParser().parseUrlPath(new PartiallyKnownString(
                ElementManipulators.getValueText(declaration), declaration, ElementManipulators.getValueTextRange(declaration)
            )).getUrlPath());
    }

    @Override
    public UrlPksParser getParser() {
        return myParser;
    }
}
