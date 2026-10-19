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
package consulo.sandboxPlugin.editor.benchmark;

import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.codeEditor.HighlighterColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.editor.highlight.SyntaxHighlighterBase;
import consulo.language.lexer.Lexer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public class EditorBenchmarkSyntaxHighlighter extends SyntaxHighlighterBase {
    private static final Map<IElementType, TextAttributesKey> KEYS = new HashMap<>();

    static {
        KEYS.put(EditorBenchmarkTokenType.KEYWORD, DefaultLanguageHighlighterColors.KEYWORD);
        KEYS.put(EditorBenchmarkTokenType.IDENTIFIER, DefaultLanguageHighlighterColors.IDENTIFIER);
        KEYS.put(EditorBenchmarkTokenType.NUMBER, DefaultLanguageHighlighterColors.NUMBER);
        KEYS.put(EditorBenchmarkTokenType.STRING, DefaultLanguageHighlighterColors.STRING);
        KEYS.put(EditorBenchmarkTokenType.CHARACTER, DefaultLanguageHighlighterColors.STRING);
        KEYS.put(EditorBenchmarkTokenType.LINE_COMMENT, DefaultLanguageHighlighterColors.LINE_COMMENT);
        KEYS.put(EditorBenchmarkTokenType.BLOCK_COMMENT, DefaultLanguageHighlighterColors.BLOCK_COMMENT);
        KEYS.put(EditorBenchmarkTokenType.DOC_COMMENT, DefaultLanguageHighlighterColors.DOC_COMMENT);
        KEYS.put(EditorBenchmarkTokenType.ANNOTATION, DefaultLanguageHighlighterColors.METADATA);
        KEYS.put(EditorBenchmarkTokenType.OPERATOR, DefaultLanguageHighlighterColors.OPERATION_SIGN);
        KEYS.put(EditorBenchmarkTokenType.BRACE, DefaultLanguageHighlighterColors.BRACES);
        KEYS.put(EditorBenchmarkTokenType.PARENTHESIS, DefaultLanguageHighlighterColors.PARENTHESES);
        KEYS.put(EditorBenchmarkTokenType.BRACKET, DefaultLanguageHighlighterColors.BRACKETS);
        KEYS.put(EditorBenchmarkTokenType.SEMICOLON, DefaultLanguageHighlighterColors.SEMICOLON);
        KEYS.put(EditorBenchmarkTokenType.COMMA, DefaultLanguageHighlighterColors.COMMA);
        KEYS.put(EditorBenchmarkTokenType.DOT, DefaultLanguageHighlighterColors.DOT);
        KEYS.put(TokenType.BAD_CHARACTER, HighlighterColors.BAD_CHARACTER);
    }

    @Override
    public Lexer getHighlightingLexer() {
        return new EditorBenchmarkLexer();
    }

    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        TextAttributesKey key = KEYS.get(tokenType);
        return key == null ? EMPTY : pack(key);
    }
}
