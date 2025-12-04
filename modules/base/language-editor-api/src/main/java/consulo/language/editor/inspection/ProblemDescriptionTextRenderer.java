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
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.psi.PsiElement;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import jakarta.annotation.Nonnull;

import java.util.function.Function;
import java.util.regex.Pattern;

import static consulo.language.editor.inspection.ProblemDescriptorUtil.FlagConstant;

/**
 * @author UNV
 * @since 2025-12-03
 */
public record ProblemDescriptionTextRenderer(@Nonnull CommonProblemDescriptor descriptor, PsiElement element, @FlagConstant int flags)
    implements Function<String, String> {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<(?:[^'\">]+|\"[^\"]*+\"|'[^']*+')*+>");

    @Override
    @RequiredReadAction
    public String apply(@Nonnull String message) {
        if ((flags & ProblemDescriptorUtil.APPEND_LINE_NUMBER) != 0
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
            String ref = ProblemDescriptorUtil.extractHighlightedText(descriptor, element);
            message = StringUtil.replace(message, "#ref", ref);
        }

        int endIndex = (flags & ProblemDescriptorUtil.TRIM_AT_END) != 0 ? message.indexOf("#end")
            : (flags & ProblemDescriptorUtil.TRIM_AT_TREE_END) != 0 ? message.indexOf("#treeend") : -1;

        if (endIndex > 0) {
            message = message.substring(0, endIndex);
        }
        message = StringUtil.replace(message, "#end", "");
        message = StringUtil.replace(message, "#treeend", "");

        message = XmlStringUtil.stripHtml(message);
        message = HTML_TAG_PATTERN.matcher(message).replaceAll("");
        message = StringUtil.unescapeXml(message).trim();

        return message;
    }
}
