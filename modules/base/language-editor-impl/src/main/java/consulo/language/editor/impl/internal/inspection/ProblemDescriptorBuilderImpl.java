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
package consulo.language.editor.impl.internal.inspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemDescriptorBase;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.util.collection.SmartList;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static consulo.language.editor.inspection.scheme.InspectionManager.ProblemDescriptorBuilder;

/**
 * @author UNV
 * @since 2025-12-02
 */
public class ProblemDescriptorBuilderImpl implements ProblemDescriptorBuilder {
    protected final LocalizeValue myDescriptionTemplate;
    protected PsiElement myStartElement, myEndElement;
    protected TextRange myRangeInElement;
    protected ProblemHighlightType myHighlightType = ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
    protected boolean myIsAfterEndOfLine = false;
    protected boolean myOnTheFly = false;
    protected boolean myShowTooltip = true;
    @Nullable
    protected List<LocalQuickFix> myLocalQuickFixes = null;

    public ProblemDescriptorBuilderImpl(LocalizeValue descriptionTemplate) {
        myDescriptionTemplate = descriptionTemplate;
    }

    
    @Override
    public ProblemDescriptorBuilder range(PsiElement element, @Nullable TextRange rangeInElement) {
        myStartElement = myEndElement = element;
        myRangeInElement = rangeInElement;
        return this;
    }

    
    @Override
    public ProblemDescriptorBuilder range(PsiElement startElement, PsiElement endElement) {
        myStartElement = startElement;
        myEndElement = endElement;
        myRangeInElement = null;
        return this;
    }

    
    @Override
    public ProblemDescriptorBuilder highlightType(ProblemHighlightType highlightType) {
        myHighlightType = highlightType;
        return this;
    }

    
    @Override
    public ProblemDescriptorBuilder afterEndOfLine() {
        myIsAfterEndOfLine = true;
        return this;
    }

    
    @Override
    public ProblemDescriptorBuilder onTheFly() {
        myOnTheFly = true;
        return this;
    }

    
    @Override
    public ProblemDescriptorBuilder showTooltip(boolean showTooltip) {
        myShowTooltip = showTooltip;
        return this;
    }

    
    @Override
    public ProblemDescriptorBuilder withFix(LocalQuickFix fix) {
        nonNullLocalQuickFixes().add(fix);
        return this;
    }

    
    @Override
    public ProblemDescriptorBuilder withFixes(Collection<? extends LocalQuickFix> localQuickFixes) {
        nonNullLocalQuickFixes().addAll(localQuickFixes);
        return this;
    }

    protected List<LocalQuickFix> nonNullLocalQuickFixes() {
        if (myLocalQuickFixes == null) {
            myLocalQuickFixes = new SmartList<>();
        }
        return myLocalQuickFixes;
    }

    
    @Override
    @RequiredReadAction
    public ProblemDescriptor create() {
        return new ProblemDescriptorBase(
            myStartElement,
            myEndElement,
            myDescriptionTemplate,
            myLocalQuickFixes == null ? LocalQuickFix.EMPTY_ARRAY : myLocalQuickFixes.toArray(LocalQuickFix.EMPTY_ARRAY),
            myHighlightType,
            myIsAfterEndOfLine,
            myRangeInElement,
            myShowTooltip,
            myOnTheFly
        );
    }
}
