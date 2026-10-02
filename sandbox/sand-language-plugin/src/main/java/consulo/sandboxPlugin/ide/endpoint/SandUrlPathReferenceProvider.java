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
package consulo.sandboxPlugin.ide.endpoint;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.endpoint.url.UrlConstants;
import consulo.endpoint.url.reference.UrlPathContext;
import consulo.endpoint.url.reference.UrlPathReferenceInjector;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.psi.util.PartiallyKnownString;
import consulo.language.util.ProcessingContext;
import consulo.sandboxPlugin.lang.psi.SandStringExpression;
import org.jspecify.annotations.Nullable;

public final class SandUrlPathReferenceProvider extends PsiReferenceProvider {
    private final UrlPathReferenceInjector<SandStringExpression> myInjector =
        UrlPathReferenceInjector.<SandStringExpression>forPartialStringFrom(SandUrlPathReferenceProvider::toPartiallyKnownString)
            .withDefaultRootContextProviderFactory(SandUrlPathReferenceProvider::createRootContext);

    @Override
    @RequiredReadAction
    public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
        if (!(element instanceof SandStringExpression expression) || toPartiallyKnownString(expression) == null) {
            return PsiReference.EMPTY_ARRAY;
        }
        return myInjector.buildAbsoluteOrRelativeReferences(expression, expression);
    }

    @RequiredReadAction
    private static @Nullable PartiallyKnownString toPartiallyKnownString(SandStringExpression expression) {
        String value = SandEndpointLiteral.getValue(expression);
        TextRange valueRange = SandEndpointLiteral.getValueRange(expression);
        SandEndpointLiteral literal = SandEndpointLiteral.parse(value);
        if (literal != null) {
            if (!literal.isClient()) {
                return null;
            }
            String url = literal.getUrl();
            TextRange urlRange = TextRange.from(valueRange.getStartOffset() + literal.getUrlOffset(), url.length());
            return new PartiallyKnownString(url, expression, urlRange);
        }
        if (SandEndpointLiteral.isUrlLike(value)) {
            return new PartiallyKnownString(value, expression, valueRange);
        }
        return null;
    }

    @RequiredReadAction
    private static UrlPathContext createRootContext(SandStringExpression expression) {
        SandEndpointLiteral literal = SandEndpointLiteral.of(expression);
        return UrlPathContext.supportingSchemes(UrlConstants.HTTP_SCHEMES, literal != null ? literal.getMethod() : null);
    }
}
