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
package consulo.language.editor.impl.internal.inspection;

import consulo.document.util.TextRange;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemBuilder;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;import jakarta.annotation.Nullable;

/**
 * @author UNV
 * @since 2024-11-03
 */
public abstract class AbstractProblemBuilder implements ProblemBuilder {
    protected final LocalizeValue myDescriptionTemplate;
    protected PsiElement myStartElement, myEndElement;
    protected TextRange myRangeInElement;
    protected ProblemHighlightType myHighlightType = ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
    protected boolean myIsAfterEndOfLine = false;
    protected boolean myOnTheFly = false;
    protected boolean myShowTooltip = true;
    protected LocalQuickFix[] myLocalQuickFixes = null;

    public AbstractProblemBuilder(LocalizeValue descriptionTemplate) {
        myDescriptionTemplate = descriptionTemplate;
    }

    @Nonnull
    @Override
    public AbstractProblemBuilder range(@Nonnull PsiElement element, @Nullable TextRange rangeInElement) {
        myStartElement = myEndElement = element;
        myRangeInElement = rangeInElement;
        return this;
    }

    @Nonnull
    @Override
    public AbstractProblemBuilder range(@Nonnull PsiElement startElement, @Nonnull PsiElement endElement) {
        myStartElement = startElement;
        myEndElement = endElement;
        myRangeInElement = null;
        return this;
    }

    @Nonnull
    @Override
    public AbstractProblemBuilder highlightType(@Nonnull ProblemHighlightType highlightType) {
        myHighlightType = highlightType;
        return this;
    }

    @Nonnull
    @Override
    public AbstractProblemBuilder afterEndOfLine() {
        myIsAfterEndOfLine = true;
        return this;
    }

    @Nonnull
    @Override
    public AbstractProblemBuilder onTheFly() {
        myOnTheFly = true;
        return this;
    }

    @Nonnull
    @Override
    public AbstractProblemBuilder showTooltip(boolean showTooltip) {
        myShowTooltip = showTooltip;
        return this;
    }

    @Nonnull
    @Override
    public AbstractProblemBuilder withFixes(LocalQuickFix... fixes) {
        if (myLocalQuickFixes != null) {
            myLocalQuickFixes = ArrayUtil.mergeArrays(myLocalQuickFixes, fixes);
        }
        else {
            myLocalQuickFixes = fixes;
        }
        return this;
    }
}