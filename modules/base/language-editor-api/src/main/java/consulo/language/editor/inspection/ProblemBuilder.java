/*
 * Copyright 2013-2024 consulo.io
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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author UNV
 * @since 2024-10-31
 */
public interface ProblemBuilder {
    @Nonnull
    default ProblemBuilder range(@Nonnull PsiElement element) {
        return range(element, element);
    }

    @Nonnull
    @RequiredReadAction
    default ProblemBuilder range(@Nonnull PsiReference reference) {
        return range(reference.getElement(), reference.getRangeInElement());
    }

    @Nonnull
    ProblemBuilder range(@Nonnull PsiElement element, @Nullable TextRange rangeInElement);

    @Nonnull
    ProblemBuilder range(@Nonnull PsiElement startElement, @Nonnull PsiElement endElement);

    @Nonnull
    ProblemBuilder highlightType(@Nonnull ProblemHighlightType highlightType);

    @Nonnull
    ProblemBuilder afterEndOfLine();

    @Nonnull
    default ProblemBuilder afterEndOfLine(boolean isAfterEndOfLine) {
        return isAfterEndOfLine ? afterEndOfLine() : this;
    }

    @Nonnull
    ProblemBuilder onTheFly();

    @Nonnull
    default ProblemBuilder onTheFly(boolean onTheFly) {
        return onTheFly ? onTheFly() : this;
    }

    @Nonnull
    default ProblemBuilder hideTooltip() {
        return showTooltip(false);
    }

    @Nonnull
    ProblemBuilder showTooltip(boolean showTooltip);

    @Nonnull
    default ProblemBuilder withFix(@Nonnull LocalQuickFix fix) {
        return withFixes(fix);
    }

    @Nonnull
    ProblemBuilder withFixes(LocalQuickFix... localQuickFixes);

    void create();
}
