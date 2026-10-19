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

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public final class EditorBenchmarkTokenType {
    public static final IElementType KEYWORD = new IElementType("EDITOR_BENCHMARK_KEYWORD", null);
    public static final IElementType IDENTIFIER = new IElementType("EDITOR_BENCHMARK_IDENTIFIER", null);
    public static final IElementType NUMBER = new IElementType("EDITOR_BENCHMARK_NUMBER", null);
    public static final IElementType STRING = new IElementType("EDITOR_BENCHMARK_STRING", null);
    public static final IElementType CHARACTER = new IElementType("EDITOR_BENCHMARK_CHARACTER", null);
    public static final IElementType LINE_COMMENT = new IElementType("EDITOR_BENCHMARK_LINE_COMMENT", null);
    public static final IElementType BLOCK_COMMENT = new IElementType("EDITOR_BENCHMARK_BLOCK_COMMENT", null);
    public static final IElementType DOC_COMMENT = new IElementType("EDITOR_BENCHMARK_DOC_COMMENT", null);
    public static final IElementType ANNOTATION = new IElementType("EDITOR_BENCHMARK_ANNOTATION", null);
    public static final IElementType OPERATOR = new IElementType("EDITOR_BENCHMARK_OPERATOR", null);
    public static final IElementType BRACE = new IElementType("EDITOR_BENCHMARK_BRACE", null);
    public static final IElementType PARENTHESIS = new IElementType("EDITOR_BENCHMARK_PARENTHESIS", null);
    public static final IElementType BRACKET = new IElementType("EDITOR_BENCHMARK_BRACKET", null);
    public static final IElementType SEMICOLON = new IElementType("EDITOR_BENCHMARK_SEMICOLON", null);
    public static final IElementType COMMA = new IElementType("EDITOR_BENCHMARK_COMMA", null);
    public static final IElementType DOT = new IElementType("EDITOR_BENCHMARK_DOT", null);

    private EditorBenchmarkTokenType() {
    }
}
