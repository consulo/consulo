/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.usage;

import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.HighlighterIterator;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.ast.IElementType;
import consulo.language.editor.highlight.*;
import consulo.language.lexer.LayeredLexer;
import consulo.language.lexer.Lexer;
import consulo.language.plain.PlainTextFileType;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

/**
 * @author Maxim.Mossienko
 * @since 2014-07-31
 */
public class SyntaxHighlighterOverEditorHighlighter implements SyntaxHighlighter {
    private final Lexer myLexer;
    private LayeredHighlighterIterator myLayeredHighlighterIterator = null;
    private final SyntaxHighlighter myHighlighter;

    public SyntaxHighlighterOverEditorHighlighter(SyntaxHighlighter _highlighter, VirtualFile file, Project project) {
        if (file.getFileType() == PlainTextFileType.INSTANCE) { // optimization for large files, PlainTextSyntaxHighlighterFactory is slow
            myHighlighter = new DefaultSyntaxHighlighter();
            myLexer = myHighlighter.getHighlightingLexer();
        }
        else {
            myHighlighter = _highlighter;
            LayeredLexer.ourDisableLayersFlag.set(Boolean.TRUE);
            EditorHighlighter editorHighlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file);

            try {
                if (editorHighlighter instanceof LayeredLexerEditorHighlighter) {
                    myLexer = new LexerEditorHighlighterLexer(editorHighlighter, false);
                }
                else {
                    myLexer = myHighlighter.getHighlightingLexer();
                }
            }
            finally {
                LayeredLexer.ourDisableLayersFlag.set(null);
            }
        }
    }

    @Nonnull
    @Override
    public Lexer getHighlightingLexer() {
        return myLexer;
    }

    @Nonnull
    @Override
    public TextAttributesKey[] getTokenHighlights(@Nonnull IElementType tokenType) {
        SyntaxHighlighter activeSyntaxHighlighter =
            myLayeredHighlighterIterator != null ? myLayeredHighlighterIterator.getActiveSyntaxHighlighter() : myHighlighter;
        return activeSyntaxHighlighter.getTokenHighlights(tokenType);
    }

    public void restart(@Nonnull CharSequence text) {
        myLexer.start(text);

        if (myLexer instanceof LexerEditorHighlighterLexer hlLexer) {
            myLayeredHighlighterIterator = hlLexer.getHighlighterIterator() instanceof LayeredHighlighterIterator hlIter ? hlIter : null;
        }
    }

    public void resetPosition(int startOffset) {
        if (myLexer instanceof LexerEditorHighlighterLexer hlLexer) {
            hlLexer.resetPosition(startOffset);
            myLayeredHighlighterIterator = hlLexer.getHighlighterIterator() instanceof LayeredHighlighterIterator hlIter ? hlIter : null;
        }
        else {
            CharSequence text = myLexer.getBufferSequence();
            myLexer.start(text, startOffset, text.length());
        }
    }
}
