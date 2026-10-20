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
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.ignore.localize.LanguageIgnoreLocalize;
import consulo.localize.LocalizeValue;
import consulo.util.io.FileUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link ColorSettingsPage} that allows to modify color scheme.
 */
@ExtensionImpl
public class IgnoreColorSettingsPage implements ColorSettingsPage {
    /**
     * The path to the sample .gitignore file.
     */
    private static final String SAMPLE_GITIGNORE_PATH = "/ignore-sample/sample.gitignore";

    /**
     * Attributes descriptor list.
     */
    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
        new AttributesDescriptor(LanguageIgnoreLocalize.highlighterHeader(), IgnoreHighlighterColors.HEADER),
        new AttributesDescriptor(LanguageIgnoreLocalize.highlighterSection(), IgnoreHighlighterColors.SECTION),
        new AttributesDescriptor(LanguageIgnoreLocalize.highlighterComment(), IgnoreHighlighterColors.COMMENT),
        new AttributesDescriptor(LanguageIgnoreLocalize.highlighterNegation(), IgnoreHighlighterColors.NEGATION),
        new AttributesDescriptor(LanguageIgnoreLocalize.highlighterBrackets(), IgnoreHighlighterColors.BRACKET),
        new AttributesDescriptor(LanguageIgnoreLocalize.highlighterSlash(), IgnoreHighlighterColors.SLASH),
        new AttributesDescriptor(LanguageIgnoreLocalize.highlighterSyntax(), IgnoreHighlighterColors.SYNTAX),
        new AttributesDescriptor(LanguageIgnoreLocalize.highlighterValue(), IgnoreHighlighterColors.VALUE),
    };

    @Override
    public SyntaxHighlighter getHighlighter() {
        return new IgnoreLanguageHighlighter(null);
    }

    @Override
    public String getDemoText() {
        try (InputStream stream = IgnoreColorSettingsPage.class.getResourceAsStream(SAMPLE_GITIGNORE_PATH)) {
            if (stream == null) {
                return "";
            }
            return FileUtil.loadTextAndClose(stream, true);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LanguageIgnoreLocalize.colorsettingsDisplayname();
    }
}
