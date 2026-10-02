// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

import consulo.endpoint.url.reference.UrlPathContext;
import consulo.endpoint.url.reference.UrlPathContextUtil;
import consulo.endpoint.url.reference.UrlPksParser;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PartiallyKnownString;
import org.jspecify.annotations.Nullable;

/**
 * Framework-specific implementations of {@link UrlPath} handling
 */
public abstract class FrameworkUrlPathSpecification {
    /**
     * @return {@link UrlPathContext} declared by given {@link PsiElement} including all parent contexts
     * e.g. class context if {@code declaration} is a url-handler method
     */
    public abstract UrlPathContext getUrlPathContext(PsiElement declaration);

    public UrlPksParser getParser() {
        UrlPksParser parser = new UrlPksParser();
        parser.setParseQueryParameters(false);
        return parser;
    }

    public UrlPath parsePath(@Nullable String path) {
        if (path == null) {
            return UrlPath.EMPTY;
        }
        return UrlPathContextUtil.chopLeadingEmptyBlock(getParser().parseUrlPath(new PartiallyKnownString(path)).getUrlPath());
    }
}
