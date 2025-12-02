/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import consulo.codeEditor.CodeInsightColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.util.TextRange;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.HighlightInfoTypeImpl;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import org.intellij.lang.annotations.MagicConstant;

public class ProblemDescriptorUtil {
    public static final int NONE = 0x00000000;
    public static final int APPEND_LINE_NUMBER = 0x00000001;
    public static final int TRIM_AT_END = 0x00000002;
    public static final int TRIM_AT_TREE_END = 0x00000004;

    @MagicConstant(flags = {NONE, APPEND_LINE_NUMBER, TRIM_AT_END, TRIM_AT_TREE_END})
    @interface FlagConstant {
    }

    @RequiredReadAction
    public static String extractHighlightedText(@Nonnull CommonProblemDescriptor descriptor, PsiElement psiElement) {
        if (psiElement == null || !psiElement.isValid()) {
            return "";
        }
        String ref = psiElement.getText();
        if (descriptor instanceof ProblemDescriptorBase problemDescriptorBase) {
            TextRange textRange = problemDescriptorBase.getTextRange();
            TextRange elementRange = psiElement.getTextRange();
            if (textRange != null) {
                textRange = textRange.shiftRight(-elementRange.getStartOffset());
                if (textRange.getStartOffset() >= 0 && textRange.getEndOffset() <= elementRange.getLength()) {
                    ref = textRange.substring(ref);
                }
            }
        }
        ref = StringUtil.replaceChar(ref, '\n', ' ').trim();
        ref = StringUtil.first(ref, 100, true);
        return ref;
    }

    @Nonnull
    @RequiredReadAction
    public static LocalizeValue renderDescriptionMessage(@Nonnull ProblemDescriptor descriptor) {
        return renderDescriptionMessage(descriptor, descriptor.getPsiElement());
    }

    @Nonnull
    @RequiredReadAction
    public static LocalizeValue renderDescriptionMessage(@Nonnull CommonProblemDescriptor descriptor, PsiElement element) {
        return renderDescriptionMessage(descriptor, element, false);
    }

    @Nonnull
    @RequiredReadAction
    public static LocalizeValue renderDescriptionMessage(
        @Nonnull CommonProblemDescriptor descriptor,
        PsiElement element,
        boolean appendLineNumber
    ) {
        return renderDescriptionMessage(descriptor, element, appendLineNumber ? APPEND_LINE_NUMBER : NONE);
    }

    @Nonnull
    @RequiredReadAction
    public static LocalizeValue renderDescriptionMessage(
        @Nonnull CommonProblemDescriptor descriptor,
        PsiElement element,
        @FlagConstant int flags
    ) {
        return descriptor.getDescriptionTemplate().map(new ProblemDescriptionTextRenderer(descriptor, element, flags));
    }

    @Nonnull
    public static HighlightInfoType highlightTypeFromDescriptor(
        @Nonnull ProblemDescriptor problemDescriptor,
        @Nonnull HighlightSeverity severity,
        @Nonnull SeverityRegistrar severityRegistrar
    ) {
        ProblemHighlightType highlightType = problemDescriptor.getHighlightType();
        switch (highlightType) {
            case GENERIC_ERROR_OR_WARNING:
                return severityRegistrar.getHighlightInfoTypeBySeverity(severity);
            case LIKE_UNKNOWN_SYMBOL:
                if (severity == HighlightSeverity.ERROR) {
                    return new HighlightInfoTypeImpl(severity, HighlightInfoType.WRONG_REF.getAttributesKey());
                }
                if (severity == HighlightSeverity.WARNING) {
                    return new HighlightInfoTypeImpl(severity, CodeInsightColors.WEAK_WARNING_ATTRIBUTES);
                }
                return severityRegistrar.getHighlightInfoTypeBySeverity(severity);
            case INFO:
                return HighlightInfoType.INFO;
            case WEAK_WARNING:
                return HighlightInfoType.WEAK_WARNING;
            case ERROR:
                return HighlightInfoType.WRONG_REF;
            case GENERIC_ERROR:
                return HighlightInfoType.ERROR;
            case WARNING:
                return HighlightInfoType.WARNING;
            case INFORMATION:
                TextAttributesKey attributes = ((ProblemDescriptorBase) problemDescriptor).getEnforcedTextAttributes();
                if (attributes != null) {
                    return new HighlightInfoTypeImpl(HighlightSeverity.INFORMATION, attributes);
                }
                return HighlightInfoType.INFORMATION;
            default: {
                HighlightInfoType type = ProblemHighlightTypeInspectionRuler.REGISTRY.get(highlightType);
                if (type != null) {
                    return new HighlightInfoTypeImpl(severity, type.getAttributesKey());
                }
                throw new RuntimeException("Cannot map " + highlightType);
            }
        }
    }
}
