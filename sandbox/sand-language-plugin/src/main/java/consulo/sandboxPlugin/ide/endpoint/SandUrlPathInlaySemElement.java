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
import consulo.endpoint.url.inlay.PsiElementUrlPathInlayHint;
import consulo.endpoint.url.inlay.UrlPathInlayHint;
import consulo.endpoint.url.inlay.UrlPathInlayHintsProviderSemElement;
import consulo.endpoint.url.reference.UrlPathContext;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.sandboxPlugin.lang.psi.SandStringExpression;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class SandUrlPathInlaySemElement implements UrlPathInlayHintsProviderSemElement {
    private final SandClass myClass;
    private final SandStringExpression myExpression;

    private SandUrlPathInlaySemElement(SandClass sandClass, SandStringExpression expression) {
        myClass = sandClass;
        myExpression = expression;
    }

    @RequiredReadAction
    public static @Nullable SandUrlPathInlaySemElement create(SandStringExpression expression) {
        SandEndpointLiteral literal = SandEndpointLiteral.of(expression);
        if (literal == null || literal.isClient()) {
            return null;
        }
        SandClass sandClass = PsiTreeUtil.getParentOfType(expression, SandClass.class);
        return sandClass != null ? new SandUrlPathInlaySemElement(sandClass, expression) : null;
    }

    @Override
    @RequiredReadAction
    public List<UrlPathInlayHint> getInlayHints() {
        SandEndpointLiteral literal = SandEndpointLiteral.of(myExpression);
        if (literal == null || literal.isClient()) {
            return List.of();
        }
        UrlPathContext context = new UrlPathContext(SandUrlTargetInfo.create(myClass, myExpression, literal)).withDeclarationFlag(true);
        return List.of(new PsiElementUrlPathInlayHint(myExpression, context));
    }
}
