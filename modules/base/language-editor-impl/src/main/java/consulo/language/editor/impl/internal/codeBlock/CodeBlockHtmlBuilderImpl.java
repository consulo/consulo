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

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.EditorHighlighter;
import consulo.colorScheme.EditorColorsScheme;
import consulo.language.Language;
import consulo.language.editor.codeBlock.CodeBlockHtmlBuilder;
import consulo.language.editor.codeBlock.CodeBlockTextHandler;
import consulo.language.editor.highlight.HighlighterFactory;
import consulo.language.editor.impl.internal.richcopy.EditorFontSegmentIterator;
import consulo.language.editor.impl.internal.richcopy.FontSegmentIterator;
import consulo.language.editor.impl.internal.richcopy.SyntaxInfoBuilder;
import consulo.language.editor.impl.internal.richcopy.WholeRangeFontSegmentIterator;
import consulo.language.editor.impl.internal.richcopy.model.SyntaxInfo;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class CodeBlockHtmlBuilderImpl implements CodeBlockHtmlBuilder {
    private static final String FILE_NAME = "code_block";

    private final Project myProject;
    private final Language myLanguage;
    private final String myCode;
    private EditorColorsScheme myColorsScheme;
    private boolean myTrimIndent;
    private float mySaturationFactor = 1;
    private boolean myGenerateBackground;
    private boolean myGenerateFontFamily;
    private boolean myGenerateWrappedTags;
    private boolean myGenerateHtmlTags;
    private boolean myGenerateFontSize;
    private @Nullable CodeBlockTextHandler myTextHandler;

    CodeBlockHtmlBuilderImpl(Project project, Language language, String code, EditorColorsScheme colorsScheme) {
        myProject = project;
        myLanguage = language;
        myCode = code;
        myColorsScheme = colorsScheme;
    }

    @Override
    public CodeBlockHtmlBuilder trimIndent(boolean trimIndent) {
        myTrimIndent = trimIndent;
        return this;
    }

    @Override
    public CodeBlockHtmlBuilder saturationFactor(float saturationFactor) {
        mySaturationFactor = saturationFactor;
        return this;
    }

    @Override
    public CodeBlockHtmlBuilder colorsScheme(EditorColorsScheme colorsScheme) {
        myColorsScheme = colorsScheme;
        return this;
    }

    @Override
    public CodeBlockHtmlBuilder generateBackground() {
        myGenerateBackground = true;
        return this;
    }

    @Override
    public CodeBlockHtmlBuilder generateFontFamily() {
        myGenerateFontFamily = true;
        return this;
    }

    @Override
    public CodeBlockHtmlBuilder generateWrappedTags() {
        myGenerateWrappedTags = true;
        return this;
    }

    @Override
    public CodeBlockHtmlBuilder generateHtmlTags() {
        myGenerateHtmlTags = true;
        return this;
    }

    @Override
    public CodeBlockHtmlBuilder generateFontSize() {
        myGenerateFontSize = true;
        return this;
    }

    @Override
    public CodeBlockHtmlBuilder textHandler(CodeBlockTextHandler textHandler) {
        myTextHandler = textHandler;
        return this;
    }

    @Override
    @RequiredReadAction
    public String build() {
        String code = (myTrimIndent ? trimIndent(myCode) : myCode).replace("\t", "    ");
        if (code.isEmpty()) {
            return "";
        }

        EditorColorsScheme scheme = mySaturationFactor == 1 ? myColorsScheme : new SaturatedColorsScheme(myColorsScheme, mySaturationFactor);

        PsiFile file = PsiFileFactory.getInstance(myProject).createFileFromText(FILE_NAME, myLanguage, code);
        EditorHighlighter highlighter = HighlighterFactory.createHighlighter(file.getViewProvider().getVirtualFile(), scheme, myProject);
        highlighter.setText(code);

        FontSegmentIterator fontSegmentIterator = myGenerateFontFamily
            ? new EditorFontSegmentIterator(scheme.getEditorFontName())
            : new WholeRangeFontSegmentIterator();
        SyntaxInfoBuilder.Context context = new SyntaxInfoBuilder.Context(code, scheme, 0, scheme.getEditorFontSize(), null);
        SyntaxInfoBuilder.MyMarkupIterator iterator = new SyntaxInfoBuilder.MyMarkupIterator(
            code,
            new SyntaxInfoBuilder.HighlighterRangeIterator(highlighter, 0, code.length()),
            fontSegmentIterator
        );
        try {
            context.iterate(iterator, code.length());
        }
        finally {
            iterator.dispose();
        }
        SyntaxInfo info = context.finish();

        CodeBlockHtmlReader reader = new CodeBlockHtmlReader(
            info,
            myGenerateBackground,
            myGenerateFontFamily,
            myGenerateWrappedTags,
            myGenerateHtmlTags,
            myGenerateFontSize,
            myTextHandler
        );
        reader.setRawText(code);
        return reader.getBuffer().toString();
    }

    /**
     * Removes the blank first and last lines and the indent the other lines share; blank lines count for no indent
     */
    static String trimIndent(String text) {
        List<String> lines = new ArrayList<>(List.of(text.split("\r\n|\r|\n", -1)));
        if (!lines.isEmpty() && lines.get(0).isBlank()) {
            lines.remove(0);
        }
        if (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) {
            lines.remove(lines.size() - 1);
        }

        int indent = Integer.MAX_VALUE;
        for (String line : lines) {
            if (!line.isBlank()) {
                int lineIndent = 0;
                while (lineIndent < line.length() && Character.isWhitespace(line.charAt(lineIndent))) {
                    lineIndent++;
                }
                indent = Math.min(indent, lineIndent);
            }
        }

        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                result.append('\n');
            }
            String line = lines.get(i);
            if (!line.isBlank()) {
                result.append(line, indent, line.length());
            }
        }
        return result.toString();
    }
}
