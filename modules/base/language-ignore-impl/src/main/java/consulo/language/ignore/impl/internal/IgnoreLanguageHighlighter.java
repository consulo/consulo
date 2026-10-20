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

import consulo.colorScheme.TextAttributesKey;
import consulo.language.ast.IElementType;
import consulo.language.editor.highlight.SyntaxHighlighterBase;
import consulo.language.ignore.IgnoreLanguage;
import consulo.language.ignore.impl.internal.lexer.IgnoreLexerAdapter;
import consulo.language.lexer.Lexer;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Syntax highlighter definition for {@link IgnoreLanguage}.
 */
public class IgnoreLanguageHighlighter extends SyntaxHighlighterBase {
    private final @Nullable VirtualFile myCurrentHighlightedFile;

    private static final Map<IElementType, TextAttributesKey> ATTRIBUTES = new HashMap<>();

    /* Binds parser definitions with highlighter colors. */
    static {
        fillMap(ATTRIBUTES, IgnoreParserDefinition.Lazy.COMMENTS, IgnoreHighlighterColors.COMMENT);
        fillMap(ATTRIBUTES, IgnoreParserDefinition.Lazy.SECTIONS, IgnoreHighlighterColors.SECTION);
        fillMap(ATTRIBUTES, IgnoreParserDefinition.Lazy.HEADERS, IgnoreHighlighterColors.HEADER);
        fillMap(ATTRIBUTES, IgnoreParserDefinition.Lazy.NEGATIONS, IgnoreHighlighterColors.NEGATION);
        fillMap(ATTRIBUTES, IgnoreParserDefinition.Lazy.BRACKETS, IgnoreHighlighterColors.BRACKET);
        fillMap(ATTRIBUTES, IgnoreParserDefinition.Lazy.SLASHES, IgnoreHighlighterColors.SLASH);
        fillMap(ATTRIBUTES, IgnoreParserDefinition.Lazy.SYNTAXES, IgnoreHighlighterColors.SYNTAX);
        fillMap(ATTRIBUTES, IgnoreParserDefinition.Lazy.VALUES, IgnoreHighlighterColors.VALUE);
    }

    public IgnoreLanguageHighlighter(@Nullable VirtualFile currentHighlightedFile) {
        myCurrentHighlightedFile = currentHighlightedFile;
    }

    @Override
    public Lexer getHighlightingLexer() {
        return new IgnoreLexerAdapter(myCurrentHighlightedFile);
    }

    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        return pack(ATTRIBUTES.get(tokenType));
    }
}
