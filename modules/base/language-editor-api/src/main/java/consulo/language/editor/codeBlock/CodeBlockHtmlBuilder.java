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

import consulo.annotation.access.RequiredReadAction;
import consulo.colorScheme.EditorColorsScheme;

/**
 * Builds the html of one block of code. By default it is just the colored text: no tags around it, no background, font family or
 * font size, so whoever shows it decides those. Its line breaks stay newlines, so it belongs into a {@code pre}.
 *
 * @see CodeBlockHighlighter#newHtmlBuilder
 */
public interface CodeBlockHtmlBuilder {
    /**
     * Removes the indent all lines of the code share, and the blank first and last lines
     */
    CodeBlockHtmlBuilder trimIndent(boolean trimIndent);

    /**
     * Makes the colors less (below one) or more (above one) vivid, keeping greys readable, as a documentation does to look calmer
     * than the editor. One by default.
     */
    CodeBlockHtmlBuilder saturationFactor(float saturationFactor);

    /**
     * The scheme to color by, the global one by default
     */
    CodeBlockHtmlBuilder colorsScheme(EditorColorsScheme colorsScheme);

    /**
     * Also colors the background of every text whose background differs from the background of the editor
     */
    CodeBlockHtmlBuilder generateBackground();

    /**
     * Also names the editor font, rather than leaving the font to whoever shows the html
     */
    CodeBlockHtmlBuilder generateFontFamily();

    /**
     * Wraps the code into a {@code div} with the editor background and foreground and a {@code pre}. Together with
     * {@link #generateHtmlTags()} it makes a whole document.
     */
    CodeBlockHtmlBuilder generateWrappedTags();

    /**
     * Wraps the code into {@code html}, {@code head} and {@code body}, see {@link #generateWrappedTags()}
     */
    CodeBlockHtmlBuilder generateHtmlTags();

    /**
     * Also declares the font size of the editor, see {@link #generateWrappedTags()}
     */
    CodeBlockHtmlBuilder generateFontSize();

    CodeBlockHtmlBuilder textHandler(CodeBlockTextHandler textHandler);

    /**
     * @return the html, empty for an empty code
     */
    @RequiredReadAction
    String build();
}
