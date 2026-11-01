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
package consulo.language.spellcheker.text;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.ast.IElementType;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiDocCommentBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.spellcheker.text.util.PsiUtils;
import consulo.language.version.LanguageVersion;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Text extractor working for any language from its {@link ParserDefinition}:
 * <ul>
 *   <li>doc comments ({@link PsiDocCommentBase}) are {@link TextContent.TextDomain#DOCUMENTATION},</li>
 *   <li>comment tokens are {@link TextContent.TextDomain#COMMENTS}; adjacent line comments of the same type are joined into one text,</li>
 *   <li>string literal elements are {@link TextContent.TextDomain#LITERALS} without the quotes.</li>
 * </ul>
 * Used for languages without their own {@link TextExtractor}; a language plugin may extend it to refine the extraction.
 *
 * @author VISTALL
 * @since 2026-11-01
 */
public abstract class CommonTextExtractor extends TextExtractor {
    private static final String COMMENT_INDENT_CHARS = " \t/*#;-!";
    private static final String COMMENT_LINE_SUFFIX_CHARS = " \t*/";
    private static final String QUOTES = "\"'`";

    private static final TextContentBuilder COMMENT_BUILDER =
        TextContentBuilder.FromPsi.removingIndents(COMMENT_INDENT_CHARS).removingLineSuffixes(COMMENT_LINE_SUFFIX_CHARS);

    @Override
    protected List<TextContent> buildTextContents(PsiElement element, Set<TextContent.TextDomain> allowedDomains) {
        return buildCommonTextContents(element, allowedDomains);
    }

    @RequiredReadAction
    public static List<TextContent> buildCommonTextContents(PsiElement element, Set<TextContent.TextDomain> allowedDomains) {
        if (element instanceof PsiDocCommentBase) {
            if (!allowedDomains.contains(TextContent.TextDomain.DOCUMENTATION)) {
                return List.of();
            }
            return asList(COMMENT_BUILDER.build(element, TextContent.TextDomain.DOCUMENTATION));
        }

        ParserDefinition parserDefinition = ParserDefinition.forLanguage(element.getLanguage());
        if (parserDefinition == null) {
            return List.of();
        }
        LanguageVersion languageVersion = element.getLanguageVersion();
        IElementType type = PsiUtilCore.getElementType(element);
        if (type == null) {
            return List.of();
        }

        if (element instanceof PsiComment && parserDefinition.getCommentTokens(languageVersion).contains(type)) {
            if (!allowedDomains.contains(TextContent.TextDomain.COMMENTS)) {
                return List.of();
            }
            List<PsiElement> roots = PsiUtils.getNotSoDistantSimilarSiblings(
                element,
                it -> it instanceof PsiComment && !(it instanceof PsiDocCommentBase) && PsiUtilCore.getElementType(it) == type
            );
            List<TextContent> fragments = new ArrayList<>();
            for (PsiElement root : roots) {
                TextContent fragment = COMMENT_BUILDER.build(root, TextContent.TextDomain.COMMENTS);
                if (fragment != null) {
                    fragments.add(fragment);
                }
            }
            return asList(TextContent.joinWithWhitespace('\n', fragments));
        }

        if (parserDefinition.getStringLiteralElements(languageVersion).contains(type)) {
            if (!allowedDomains.contains(TextContent.TextDomain.LITERALS)) {
                return List.of();
            }
            return asList(excludeQuotes(TextContentBuilder.FromPsi.build(element, TextContent.TextDomain.LITERALS)));
        }
        return List.of();
    }

    private static @Nullable TextContent excludeQuotes(@Nullable TextContent content) {
        if (content == null || content.length() < 2) {
            return content;
        }
        TextContent result = content;
        if (QUOTES.indexOf(result.charAt(result.length() - 1)) >= 0) {
            result = result.excludeRange(new TextRange(result.length() - 1, result.length()));
        }
        if (result.length() > 0 && QUOTES.indexOf(result.charAt(0)) >= 0) {
            result = result.excludeRange(new TextRange(0, 1));
        }
        return result.trimWhitespace();
    }

    private static List<TextContent> asList(@Nullable TextContent content) {
        return content == null ? List.of() : List.of(content);
    }
}
