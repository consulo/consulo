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
package consulo.language.editor.inspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * @author UNV
 * @since 2025-11-09
 */
public class ProblemBuilderWrapper implements ProblemBuilder {
    
    protected final ProblemBuilder mySubBuilder;

    public ProblemBuilderWrapper(ProblemBuilder subBuilder) {
        mySubBuilder = subBuilder;
    }

    
    @Override
    public ProblemBuilder range(PsiElement element) {
        return rewrap(mySubBuilder.range(element));
    }

    
    @Override
    public ProblemBuilder range(PsiElement element, @Nullable TextRange rangeInElement) {
        return rewrap(mySubBuilder.range(element, rangeInElement));
    }

    
    @Override
    public ProblemBuilder range(PsiElement startElement, PsiElement endElement) {
        return rewrap(mySubBuilder.range(startElement, endElement));
    }

    
    @Override
    @RequiredReadAction
    public ProblemBuilder rangeByRef(PsiReference reference) {
        return rewrap(mySubBuilder.rangeByRef(reference));
    }

    
    @Override
    public ProblemBuilder highlightType(ProblemHighlightType highlightType) {
        return rewrap(mySubBuilder.highlightType(highlightType));
    }

    
    @Override
    public ProblemBuilder afterEndOfLine() {
        return rewrap(mySubBuilder.afterEndOfLine());
    }

    
    @Override
    public ProblemBuilder afterEndOfLine(boolean isAfterEndOfLine) {
        return rewrap(mySubBuilder.afterEndOfLine(isAfterEndOfLine));
    }

    
    @Override
    public ProblemBuilder onTheFly() {
        return rewrap(mySubBuilder.onTheFly());
    }

    
    @Override
    public ProblemBuilder onTheFly(boolean onTheFly) {
        return rewrap(mySubBuilder.onTheFly(onTheFly));
    }

    
    @Override
    public ProblemBuilder hideTooltip() {
        return rewrap(mySubBuilder.hideTooltip());
    }

    
    @Override
    public ProblemBuilder showTooltip(boolean showTooltip) {
        return rewrap(mySubBuilder.showTooltip(showTooltip));
    }

    
    @Override
    public ProblemBuilder withFix(LocalQuickFix fix) {
        return rewrap(mySubBuilder.withFix(fix));
    }

    @Override
    public ProblemBuilder withOptionalFix(@Nullable LocalQuickFix fix) {
        return rewrap(mySubBuilder.withOptionalFix(fix));
    }

    
    @Override
    public ProblemBuilder withFixes(LocalQuickFix[] fixes) {
        return rewrap(mySubBuilder.withFixes(fixes));
    }

    
    @Override
    public ProblemBuilder withFixes(Collection<? extends LocalQuickFix> localQuickFixes) {
        return rewrap(mySubBuilder.withFixes(localQuickFixes));
    }

    @Override
    public void create() {
        mySubBuilder.create();
    }

    protected ProblemBuilder rewrap(ProblemBuilder subBuilder) {
        if (subBuilder != mySubBuilder) {
            throw new IllegalStateException("Expecting builder to stay the same object; or override rewrap() to handle builder change");
        }
        return this;
    }
}
