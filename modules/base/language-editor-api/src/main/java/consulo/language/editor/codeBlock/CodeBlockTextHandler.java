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
package consulo.language.editor.codeBlock;

/**
 * Writes a piece of text of the code instead of the builder, to wrap or replace it
 *
 * @see CodeBlockHtmlBuilder#textHandler(CodeBlockTextHandler)
 */
@FunctionalInterface
public interface CodeBlockTextHandler {
    /**
     * @param startOffset  the start of the piece in the code
     * @param endOffset    the end of the piece in the code
     * @param resultBuffer the html written so far
     * @param superHandler writes the piece as the builder would, escaped and styled
     */
    void handleText(int startOffset, int endOffset, StringBuilder resultBuffer, Runnable superHandler);
}
