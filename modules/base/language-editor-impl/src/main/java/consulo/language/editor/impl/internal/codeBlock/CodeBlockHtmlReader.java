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
package consulo.language.editor.impl.internal.codeBlock;

import consulo.language.editor.codeBlock.CodeBlockTextHandler;
import consulo.language.editor.impl.internal.richcopy.model.SyntaxInfo;
import consulo.language.editor.impl.internal.richcopy.view.HtmlSyntaxInfoReader;
import org.jspecify.annotations.Nullable;

/**
 * Writes only the parts of the html a {@link CodeBlockHtmlBuilderImpl} asked for
 */
class CodeBlockHtmlReader extends HtmlSyntaxInfoReader {
    private static final int TAB_SIZE = 2;

    private final boolean myGenerateBackground;
    private final boolean myGenerateFontFamily;
    private final boolean myGenerateWrappedTags;
    private final boolean myGenerateHtmlTags;
    private final boolean myGenerateFontSize;
    private final @Nullable CodeBlockTextHandler myTextHandler;

    CodeBlockHtmlReader(
        SyntaxInfo info,
        boolean generateBackground,
        boolean generateFontFamily,
        boolean generateWrappedTags,
        boolean generateHtmlTags,
        boolean generateFontSize,
        @Nullable CodeBlockTextHandler textHandler
    ) {
        super(info, TAB_SIZE);
        myGenerateBackground = generateBackground;
        myGenerateFontFamily = generateFontFamily;
        myGenerateWrappedTags = generateWrappedTags;
        myGenerateHtmlTags = generateHtmlTags;
        myGenerateFontSize = generateFontSize;
        myTextHandler = textHandler;
    }

    @Override
    protected void generateCloseBodyHtmlTags() {
        if (myGenerateHtmlTags) {
            super.generateCloseBodyHtmlTags();
        }
    }

    @Override
    protected void appendOpenBodyHtmlTags() {
        if (myGenerateHtmlTags) {
            super.appendOpenBodyHtmlTags();
        }
    }

    @Override
    protected void appendFontSizeRule() {
        if (myGenerateFontSize) {
            super.appendFontSizeRule();
        }
    }

    @Override
    protected void appendCloseTags() {
        if (myGenerateWrappedTags) {
            super.appendCloseTags();
        }
    }

    @Override
    protected void appendStartTags() {
        if (myGenerateWrappedTags) {
            super.appendStartTags();
        }
    }

    /**
     * A code block always lands in a {@code pre}, where a {@code br} is spaced as a paragraph by some renderers
     */
    @Override
    protected void appendLineBreak() {
        myResultBuffer.append('\n');
    }

    @Override
    protected void defineBackground(int id, StringBuilder styleBuffer) {
        if (myGenerateBackground) {
            super.defineBackground(id, styleBuffer);
        }
    }

    @Override
    protected void appendFontFamilyRule(StringBuilder styleBuffer, int fontFamilyId) {
        if (myGenerateFontFamily) {
            super.appendFontFamilyRule(styleBuffer, fontFamilyId);
        }
    }

    @Override
    public void handleText(int startOffset, int endOffset) {
        if (myTextHandler != null) {
            myTextHandler.handleText(startOffset, endOffset, myResultBuffer, () -> super.handleText(startOffset, endOffset));
        }
        else {
            super.handleText(startOffset, endOffset);
        }
    }
}
