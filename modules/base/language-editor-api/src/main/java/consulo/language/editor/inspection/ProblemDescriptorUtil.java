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
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.HighlightInfoTypeImpl;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import org.intellij.lang.annotations.MagicConstant;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class ProblemDescriptorUtil {
    public static final int NONE = 0x00000000;
    public static final int APPEND_LINE_NUMBER = 0x00000001;
    public static final int TRIM_AT_END = 0x00000002;
    public static final int TRIM_AT_TREE_END = 0x00000004;

    @MagicConstant(flags = {NONE, APPEND_LINE_NUMBER, TRIM_AT_END, TRIM_AT_TREE_END})
    @interface FlagConstant {
    }

    public static Couple<String> XML_CODE_MARKER = Couple.of("<xml-code>", "</xml-code>");

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
    public static LocalizeValue renderDescriptionMessage(
        @Nonnull CommonProblemDescriptor descriptor,
        PsiElement element,
        boolean appendLineNumber
    ) {
        return renderDescriptionMessage(descriptor, element, appendLineNumber ? APPEND_LINE_NUMBER : NONE);
    }

    private record DescriptionRenderingMapper(@Nonnull CommonProblemDescriptor descriptor, PsiElement element, @FlagConstant int flags)
        implements BiFunction<LocalizeManager, String, String> {

        @Override
        @RequiredReadAction
        public String apply(LocalizeManager localizeManager, @Nonnull String message) {
            if ((flags & APPEND_LINE_NUMBER) != 0
                && descriptor instanceof ProblemDescriptor problemDescriptor
                && !message.contains("#ref")
                && message.contains("#loc")) {
                int lineNumber = problemDescriptor.getLineNumber();
                if (lineNumber >= 0) {
                    message = StringUtil.replace(
                        message,
                        "#loc",
                        "(" + InspectionLocalize.inspectionExportResultsAtLine() + " " + lineNumber + ")"
                    );
                }
            }
            message = StringUtil.replace(message, "<code>", "'");
            message = StringUtil.replace(message, "</code>", "'");
            message = StringUtil.replace(message, "#loc ", "");
            message = StringUtil.replace(message, " #loc", "");
            message = StringUtil.replace(message, "#loc", "");
            if (message.contains("#ref")) {
                String ref = extractHighlightedText(descriptor, element);
                message = StringUtil.replace(message, "#ref", ref);
            }

            int endIndex =
                (flags & TRIM_AT_END) != 0 ? message.indexOf("#end") : (flags & TRIM_AT_TREE_END) != 0 ? message.indexOf("#treeend") : -1;
            if (endIndex > 0) {
                message = message.substring(0, endIndex);
            }
            message = StringUtil.replace(message, "#end", "");
            message = StringUtil.replace(message, "#treeend", "");

            if (message.contains(XML_CODE_MARKER.first)) {
                message = unescapeXmlCode(message);
            }
            else {
                message = StringUtil.unescapeXml(message).trim();
            }
            return message;
        }
    }

    @Nonnull
    @RequiredReadAction
    public static LocalizeValue renderDescriptionMessage(
        @Nonnull CommonProblemDescriptor descriptor,
        PsiElement element,
        @FlagConstant int flags
    ) {
        return descriptor.getDescriptionTemplate().map(new DescriptionRenderingMapper(descriptor, element, flags));
    }

    private static String unescapeXmlCode(String message) {
        List<String> strings = new ArrayList<>();
        for (String string : StringUtil.split(message, XML_CODE_MARKER.first)) {
            if (string.contains(XML_CODE_MARKER.second)) {
                strings.addAll(StringUtil.split(string, XML_CODE_MARKER.second, false));
            }
            else {
                strings.add(string);
            }
        }
        StringBuilder builder = new StringBuilder();
        for (String string : strings) {
            if (string.contains(XML_CODE_MARKER.second)) {
                builder.append(string.replace(XML_CODE_MARKER.second, ""));
            }
            else {
                builder.append(StringUtil.unescapeXml(string));
            }
        }
        return builder.toString();
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
