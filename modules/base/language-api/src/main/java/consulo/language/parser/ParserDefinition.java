/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.parser;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IFileElementType;
import consulo.language.ast.TokenSet;
import consulo.language.ast.TokenType;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.file.FileViewProvider;
import consulo.language.lexer.Lexer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.version.LanguageVersion;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Defines the implementation of a parser for a custom language.
 *
 * @see #forLanguage(Language)
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ParserDefinition extends LanguageExtension {
    ExtensionPointCacheKey<ParserDefinition, ByLanguageValue<ParserDefinition>> KEY =
        ExtensionPointCacheKey.create("ParserDefinition", LanguageOneToOne.build());

    @Nullable
    static ParserDefinition forLanguage(@Nonnull Language language) {
        return Application.get().getExtensionPoint(ParserDefinition.class).getOrBuildCache(KEY).get(language);
    }

    /**
     * Language of parser definition
     */
    @Override
    @Nonnull
    Language getLanguage();

    /**
     * Returns the lexer for lexing files in the specified project. This lexer does not need to support incremental relexing - it is always
     * called for the entire file.
     *
     * @param languageVersion version of language
     * @return the lexer instance.
     */
    @Nonnull
    Lexer createLexer(@Nonnull LanguageVersion languageVersion);

    /**
     * Returns the parser for parsing files in the specified project.
     *
     * @param languageVersion version of language
     * @return the parser instance.
     */
    @Nonnull
    PsiParser createParser(@Nonnull LanguageVersion languageVersion);

    /**
     * Returns the element type of the node describing a file in the specified language.
     *
     * @return the file node element type.
     */
    @Nonnull
    IFileElementType getFileNodeType();

    /**
     * Returns the set of token types which are treated as whitespace by the PSI builder.
     * Tokens of those types are automatically skipped by PsiBuilder. Whitespace elements
     * on the bounds of nodes built by PsiBuilder are automatically excluded from the text
     * range of the nodes.
     * <p><strong>It is strongly advised you return TokenSet that only contains {@link TokenType#WHITE_SPACE},
     * which is suitable for all the languages unless you really need to use special whitespace token</strong>
     *
     * @param languageVersion version of language
     * @return the set of whitespace token types.
     */
    @Nonnull
    default TokenSet getWhitespaceTokens(@Nonnull LanguageVersion languageVersion) {
        return TokenSet.WHITE_SPACE;
    }

    /**
     * Returns the set of token types which are treated as comments by the PSI builder.
     * Tokens of those types are automatically skipped by PsiBuilder. Also, To Do patterns
     * are searched in the text of tokens of those types.
     *
     * @param languageVersion version of language
     * @return the set of comment token types.
     */
    @Nonnull
    TokenSet getCommentTokens(@Nonnull LanguageVersion languageVersion);

    /**
     * Returns the set of element types which are treated as string literals. "Search in strings"
     * option in refactorings is applied to the contents of such tokens.
     *
     * @param languageVersion version of language
     * @return the set of string literal element types.
     */
    @Nonnull
    TokenSet getStringLiteralElements(@Nonnull LanguageVersion languageVersion);

    /**
     * Creates a PSI element for the specified AST node. The AST tree is a simple, semantic-free
     * tree of AST nodes which is built during the PsiBuilder parsing pass. The PSI tree is built
     * over the AST tree and includes elements of different types for different language constructs.
     *
     * @param node the node for which the PSI element should be returned.
     * @return the PSI element matching the element type of the AST node.
     */
    @Nonnull
    @RequiredReadAction
    default PsiElement createElement(@Nonnull ASTNode node) {
        throw new UnsupportedOperationException("#createElement() is not implemented for elementType: " + node.getElementType());
    }

    /**
     * Creates a PSI element for the specified virtual file.
     *
     * @param viewProvider virtual file.
     * @return the PSI file element.
     */
    @Nonnull
    PsiFile createFile(@Nonnull FileViewProvider viewProvider);

    /**
     * Checks if the specified two token types need to be separated by a space according to the language grammar.
     * For example, in Java two keywords are always separated by a space; a keyword and an opening parenthesis may
     * be separated or not separated. This is used for automatic whitespace insertion during AST modification operations.
     *
     * @param left  the first token to check.
     * @param right the second token to check.
     * @return the spacing requirements.
     */
    @Nonnull
    default SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
        //noinspection deprecation
        return spaceExistanceTypeBetweenTokens(left, right);
    }

    /**
     * @deprecated Override {@link ParserDefinition#spaceExistenceTypeBetweenTokens(ASTNode, ASTNode)} instead
     */
    @Deprecated
    @Nonnull
    default SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }

    /**
     * Requirements for spacing between tokens.
     *
     * @see ParserDefinition#spaceExistenceTypeBetweenTokens
     */
    enum SpaceRequirements {
        /**
         * Whitespace between tokens is optional.
         */
        MAY,
        /**
         * Whitespace between tokens is required.
         */
        MUST,
        /**
         * Whitespace between tokens is not allowed.
         */
        MUST_NOT,
        /**
         * A line break is required between tokens.
         */
        MUST_LINE_BREAK,
    }
}
