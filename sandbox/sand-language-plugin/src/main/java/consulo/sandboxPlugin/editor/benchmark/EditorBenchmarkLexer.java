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

import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.lexer.LexerBase;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public class EditorBenchmarkLexer extends LexerBase {
    private static final Set<String> KEYWORDS = Set.of(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do",
        "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
        "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static",
        "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
        "true", "false", "null", "var", "record", "yield"
    );

    private static final String OPERATOR_CHARS = "+-*/%=<>!&|^~?:";

    private CharSequence myBuffer = "";
    private int myBufferEnd;
    private int myTokenStart;
    private int myTokenEnd;
    private @Nullable IElementType myTokenType;

    @Override
    public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
        myBuffer = buffer;
        myBufferEnd = endOffset;
        myTokenEnd = startOffset;
        advance();
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public @Nullable IElementType getTokenType() {
        return myTokenType;
    }

    @Override
    public int getTokenStart() {
        return myTokenStart;
    }

    @Override
    public int getTokenEnd() {
        return myTokenEnd;
    }

    @Override
    public void advance() {
        myTokenStart = myTokenEnd;
        if (myTokenStart >= myBufferEnd) {
            myTokenType = null;
            return;
        }

        char c = myBuffer.charAt(myTokenStart);
        int next = myTokenStart + 1;

        if (Character.isWhitespace(c)) {
            while (next < myBufferEnd && Character.isWhitespace(myBuffer.charAt(next))) {
                next++;
            }
            finish(next, TokenType.WHITE_SPACE);
        }
        else if (c == '/' && charAt(next) == '/') {
            while (next < myBufferEnd && myBuffer.charAt(next) != '\n') {
                next++;
            }
            finish(next, EditorBenchmarkTokenType.LINE_COMMENT);
        }
        else if (c == '/' && charAt(next) == '*') {
            boolean doc = charAt(next + 1) == '*' && charAt(next + 2) != '/';
            int close = indexOfCommentEnd(next + 1);
            finish(close < 0 ? myBufferEnd : close + 2, doc ? EditorBenchmarkTokenType.DOC_COMMENT : EditorBenchmarkTokenType.BLOCK_COMMENT);
        }
        else if (c == '"' || c == '\'') {
            finish(skipQuoted(next, c), c == '"' ? EditorBenchmarkTokenType.STRING : EditorBenchmarkTokenType.CHARACTER);
        }
        else if (Character.isDigit(c)) {
            while (next < myBufferEnd && isNumberPart(myBuffer.charAt(next))) {
                next++;
            }
            finish(next, EditorBenchmarkTokenType.NUMBER);
        }
        else if (Character.isJavaIdentifierStart(c)) {
            while (next < myBufferEnd && Character.isJavaIdentifierPart(myBuffer.charAt(next))) {
                next++;
            }
            String word = myBuffer.subSequence(myTokenStart, next).toString();
            finish(next, KEYWORDS.contains(word) ? EditorBenchmarkTokenType.KEYWORD : EditorBenchmarkTokenType.IDENTIFIER);
        }
        else if (c == '@' && next < myBufferEnd && Character.isJavaIdentifierStart(myBuffer.charAt(next))) {
            while (next < myBufferEnd && Character.isJavaIdentifierPart(myBuffer.charAt(next))) {
                next++;
            }
            finish(next, EditorBenchmarkTokenType.ANNOTATION);
        }
        else if (c == '{' || c == '}') {
            finish(next, EditorBenchmarkTokenType.BRACE);
        }
        else if (c == '(' || c == ')') {
            finish(next, EditorBenchmarkTokenType.PARENTHESIS);
        }
        else if (c == '[' || c == ']') {
            finish(next, EditorBenchmarkTokenType.BRACKET);
        }
        else if (c == ';') {
            finish(next, EditorBenchmarkTokenType.SEMICOLON);
        }
        else if (c == ',') {
            finish(next, EditorBenchmarkTokenType.COMMA);
        }
        else if (c == '.') {
            finish(next, EditorBenchmarkTokenType.DOT);
        }
        else if (OPERATOR_CHARS.indexOf(c) >= 0) {
            while (next < myBufferEnd && OPERATOR_CHARS.indexOf(myBuffer.charAt(next)) >= 0) {
                next++;
            }
            finish(next, EditorBenchmarkTokenType.OPERATOR);
        }
        else {
            finish(next, TokenType.BAD_CHARACTER);
        }
    }

    @Override
    public CharSequence getBufferSequence() {
        return myBuffer;
    }

    @Override
    public int getBufferEnd() {
        return myBufferEnd;
    }

    private void finish(int end, IElementType type) {
        myTokenEnd = end;
        myTokenType = type;
    }

    private char charAt(int offset) {
        return offset < myBufferEnd ? myBuffer.charAt(offset) : '\0';
    }

    private int indexOfCommentEnd(int from) {
        for (int i = from; i + 1 < myBufferEnd; i++) {
            if (myBuffer.charAt(i) == '*' && myBuffer.charAt(i + 1) == '/') {
                return i;
            }
        }
        return -1;
    }

    private int skipQuoted(int from, char quote) {
        int i = from;
        while (i < myBufferEnd) {
            char c = myBuffer.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == quote) {
                return i + 1;
            }
            if (c == '\n') {
                return i;
            }
            i++;
        }
        return myBufferEnd;
    }

    private static boolean isNumberPart(char c) {
        return Character.isLetterOrDigit(c) || c == '.' || c == '_';
    }
}
