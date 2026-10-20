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

import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.colorScheme.TextAttributesKey;

/**
 * Contains highlighter attributes definitions.
 */
public class IgnoreHighlighterColors {
    /**
     * Default style for regular comment started with #
     */
    public static final TextAttributesKey COMMENT =
        TextAttributesKey.createTextAttributesKey("IGNORE.COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);

    /**
     * Default style for section comment started with ##
     */
    public static final TextAttributesKey SECTION =
        TextAttributesKey.createTextAttributesKey("IGNORE.SECTION", DefaultLanguageHighlighterColors.LINE_COMMENT);

    /**
     * Default style for header comment started with ###
     */
    public static final TextAttributesKey HEADER =
        TextAttributesKey.createTextAttributesKey("IGNORE.HEADER", DefaultLanguageHighlighterColors.LINE_COMMENT);

    /**
     * Default style for negation element - ! in the beginning of the entry
     */
    public static final TextAttributesKey NEGATION =
        TextAttributesKey.createTextAttributesKey("IGNORE.NEGATION", DefaultLanguageHighlighterColors.KEYWORD);

    /**
     * Default style for brackets - [ ]
     */
    public static final TextAttributesKey BRACKET =
        TextAttributesKey.createTextAttributesKey("IGNORE.BRACKET", DefaultLanguageHighlighterColors.BRACKETS);

    /**
     * Default style for slash - /
     */
    public static final TextAttributesKey SLASH =
        TextAttributesKey.createTextAttributesKey("IGNORE.SLASH", DefaultLanguageHighlighterColors.OPERATION_SIGN);

    /**
     * Default style for syntax element - syntax:
     */
    public static final TextAttributesKey SYNTAX =
        TextAttributesKey.createTextAttributesKey("IGNORE.SYNTAX", DefaultLanguageHighlighterColors.METADATA);

    /**
     * Default style for value - parts of paths
     */
    public static final TextAttributesKey VALUE =
        TextAttributesKey.createTextAttributesKey("IGNORE.VALUE", DefaultLanguageHighlighterColors.STRING);
}
