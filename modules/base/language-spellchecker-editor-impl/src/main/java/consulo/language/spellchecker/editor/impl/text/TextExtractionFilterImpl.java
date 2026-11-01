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
package consulo.language.spellchecker.editor.impl.text;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.document.util.TextRange;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.SuppressionUtil;
import consulo.language.psi.ElementManipulator;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.spellcheker.text.TextContent;
import consulo.language.spellcheker.text.TextExtractionFilter;
import consulo.util.lang.StringUtil;
import jakarta.inject.Singleton;

import java.util.regex.Pattern;

/**
 * @author VISTALL
 * @since 2026-11-01
 */
@Singleton
@ServiceImpl
public class TextExtractionFilterImpl implements TextExtractionFilter {
    private static final Pattern NOINSPECTION_SUPPRESSION = Pattern.compile(SuppressionUtil.COMMON_SUPPRESS_REGEXP);
    private static final Pattern PROPERTY_SUPPRESSION =
        Pattern.compile("\\s*suppress inspection \"" + LocalInspectionTool.VALID_ID_PATTERN + "\"");
    private static final Pattern LICENSE_PATTERN = Pattern.compile("(?i)License.*?(as is|MIT|GNU|GPL|Apache|BSD)", Pattern.DOTALL);
    private static final Pattern KEY_PATTERN = Pattern.compile("-----BEGIN PUBLIC KEY-----.*?(-----END PUBLIC KEY-----)", Pattern.DOTALL);

    @Override
    @RequiredReadAction
    public boolean isSuppressionComment(PsiElement element) {
        if (!(element instanceof PsiComment)) {
            return false;
        }
        ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(element);
        if (manipulator == null) {
            return false;
        }
        TextRange valueRange = manipulator.getRangeInElement(element);
        String commentText = valueRange.substring(element.getText());
        return NOINSPECTION_SUPPRESSION.matcher(commentText).matches();
    }

    @Override
    public boolean shouldIgnore(TextContent content) {
        return isSuppressionComment(content) || isCopyrightComment(content) || isKeyLike(content);
    }

    private static boolean isKeyLike(TextContent content) {
        return content.getDomain() == TextContent.TextDomain.PLAIN_TEXT && KEY_PATTERN.matcher(content.toString()).matches();
    }

    private static boolean isCopyrightComment(TextContent content) {
        return (content.getDomain() == TextContent.TextDomain.COMMENTS || content.getDomain() == TextContent.TextDomain.DOCUMENTATION)
            && StringUtil.containsIgnoreCase(content.toString(), "Copyright")
            && (isAtFileStart(content) || LICENSE_PATTERN.matcher(content).find());
    }

    private static boolean isAtFileStart(TextContent content) {
        PsiFile file = content.getContainingFile();
        int textStart = content.textOffsetToFile(0);
        return file.getViewProvider().getContents().subSequence(0, textStart).chars().noneMatch(Character::isLetterOrDigit);
    }

    private static boolean isSuppressionComment(TextContent content) {
        return content.getDomain() == TextContent.TextDomain.COMMENTS
            && (NOINSPECTION_SUPPRESSION.matcher(content).lookingAt() || PROPERTY_SUPPRESSION.matcher(content).lookingAt());
    }
}
