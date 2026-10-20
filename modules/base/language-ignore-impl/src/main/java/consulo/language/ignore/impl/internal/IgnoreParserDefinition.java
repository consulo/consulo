/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 hsz Jakub Chrzanowski <jakub@hsz.mobi>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package consulo.language.ignore.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IFileElementType;
import consulo.language.ast.TokenSet;
import consulo.language.file.FileViewProvider;
import consulo.language.ignore.IgnoreFileType;
import consulo.language.ignore.IgnoreLanguage;
import consulo.language.ignore.impl.internal.lexer.IgnoreLexerAdapter;
import consulo.language.ignore.impl.internal.parser.IgnoreParser;
import consulo.language.ignore.impl.internal.psi.IgnoreFileImpl;
import consulo.language.ignore.impl.internal.psi.IgnoreTypesFactory;
import consulo.language.ignore.psi.IgnoreTypes;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiParser;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.version.LanguageVersion;

/**
 * Defines the implementation of a parser for a custom language.
 */
@ExtensionImpl
public class IgnoreParserDefinition implements ParserDefinition {
    public static final class Lazy {
        /**
         * Regular comment started with #
         */
        public static final TokenSet COMMENTS = TokenSet.create(IgnoreTypes.COMMENT);

        /**
         * Section comment started with ##
         */
        public static final TokenSet SECTIONS = TokenSet.create(IgnoreTypes.SECTION);

        /**
         * Header comment started with ###
         */
        public static final TokenSet HEADERS = TokenSet.create(IgnoreTypes.HEADER);

        /**
         * Negation element - ! in the beginning of the entry
         */
        public static final TokenSet NEGATIONS = TokenSet.create(IgnoreTypes.NEGATION);

        /**
         * Brackets []
         */
        public static final TokenSet BRACKETS = TokenSet.create(IgnoreTypes.BRACKET_LEFT, IgnoreTypes.BRACKET_RIGHT);

        /**
         * Slashes /
         */
        public static final TokenSet SLASHES = TokenSet.create(IgnoreTypes.SLASH);

        /**
         * Syntax syntax: type
         */
        public static final TokenSet SYNTAXES = TokenSet.create(IgnoreTypes.SYNTAX_KEY);

        /**
         * All values - parts of paths
         */
        public static final TokenSet VALUES = TokenSet.create(IgnoreTypes.VALUE);
    }

    /**
     * Element type of the node describing a file in the specified language.
     */
    public static final IFileElementType FILE = new IFileElementType(IgnoreLanguage.INSTANCE);

    @Override
    public Language getLanguage() {
        return IgnoreLanguage.INSTANCE;
    }

    /**
     * Returns the lexer for lexing files in the specified project. This lexer does not need to support incremental
     * relexing - it is always called for the entire file.
     *
     * @param languageVersion the language version.
     * @return the lexer instance.
     */
    @Override
    public Lexer createLexer(LanguageVersion languageVersion) {
        return new IgnoreLexerAdapter();
    }

    /**
     * Returns the parser for parsing files in the specified project.
     *
     * @param languageVersion the language version.
     * @return the parser instance.
     */
    @Override
    public PsiParser createParser(LanguageVersion languageVersion) {
        return new IgnoreParser();
    }

    /**
     * Returns the element type of the node describing a file in the specified language.
     *
     * @return the file node element type.
     */
    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    /**
     * Returns the set of token types which are treated as comments by the PSI builder.
     * Tokens of those types are automatically skipped by PsiBuilder. Also, To Do patterns
     * are searched in the text of tokens of those types.
     *
     * @param languageVersion the language version.
     * @return the set of comment token types.
     */
    @Override
    public TokenSet getCommentTokens(LanguageVersion languageVersion) {
        return Lazy.COMMENTS;
    }

    /**
     * Returns the set of element types which are treated as string literals. "Search in strings"
     * option in refactorings is applied to the contents of such tokens.
     *
     * @param languageVersion the language version.
     * @return the set of string literal element types.
     */
    @Override
    public TokenSet getStringLiteralElements(LanguageVersion languageVersion) {
        return TokenSet.EMPTY;
    }

    /**
     * Creates a PSI element for the specified AST node. The AST tree is a simple, semantic-free
     * tree of AST nodes which is built during the PsiBuilder parsing pass. The PSI tree is built
     * over the AST tree and includes elements of different types for different language constructs.
     *
     * @param node the node for which the PSI element should be returned.
     * @return the PSI element matching the element type of the AST node.
     */
    @Override
    public PsiElement createElement(ASTNode node) {
        return IgnoreTypesFactory.createElement(node);
    }

    /**
     * Creates a PSI element for the specified virtual file.
     *
     * @param viewProvider virtual file.
     * @return the PSI file element.
     */
    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        if (viewProvider.getBaseLanguage() instanceof IgnoreLanguage ignoreLanguage) {
            return new IgnoreFileImpl(viewProvider, ignoreLanguage.getFileType());
        }
        return new IgnoreFileImpl(viewProvider, IgnoreFileType.INSTANCE);
    }
}
