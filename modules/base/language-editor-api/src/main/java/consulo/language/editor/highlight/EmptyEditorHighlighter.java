// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.highlight;

import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.HighlighterClient;
import consulo.codeEditor.HighlighterColors;
import consulo.codeEditor.HighlighterIterator;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.internal.PrioritizedDocumentListener;
import consulo.language.ast.IElementType;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class EmptyEditorHighlighter implements EditorHighlighter, PrioritizedDocumentListener {
    private static final Logger LOG = Logger.getInstance(EmptyEditorHighlighter.class);

    private TextAttributes myCachedAttributes;
    private final TextAttributesKey myKey;
    private int myTextLength = 0;
    private HighlighterClient myEditor;

    public EmptyEditorHighlighter() {
        this(null, HighlighterColors.TEXT);
    }

    public EmptyEditorHighlighter(@Nullable EditorColorsScheme scheme, @Nonnull TextAttributesKey key) {
        myKey = key;
        myCachedAttributes = scheme != null ? scheme.getAttributes(key) : null;
    }

    public EmptyEditorHighlighter(@Nullable TextAttributes attributes) {
        myCachedAttributes = attributes;
        myKey = HighlighterColors.TEXT;
    }

    @Override
    public void setText(@Nonnull CharSequence text) {
        myTextLength = text.length();
    }

    @Override
    public void setEditor(@Nonnull HighlighterClient editor) {
        LOG.assertTrue(myEditor == null, "Highlighters cannot be reused with different editors");
        myEditor = editor;
    }

    @Override
    public void setColorScheme(@Nonnull EditorColorsScheme scheme) {
        myCachedAttributes = scheme.getAttributes(myKey);
    }

    @Override
    public void documentChanged(@Nonnull DocumentEvent e) {
        myTextLength += e.getNewLength() - e.getOldLength();
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public @Nonnull HighlighterIterator createIterator(int startOffset) {
        return new HighlighterIterator() {
            private final TextAttributesKey[] myKeys = new TextAttributesKey[]{myKey};
            private int index = 0;

            @Override
            public TextAttributes getTextAttributes() {
                return myCachedAttributes;
            }

            @Override
            @Nonnull
            public TextAttributesKey[] getTextAttributesKeys() {
                return myKeys;
            }

            @Override
            public int getStart() {
                return 0;
            }

            @Override
            public int getEnd() {
                return myTextLength;
            }

            @Override
            public void advance() {
                index++;
            }

            @Override
            public void retreat() {
                index--;
            }

            @Override
            public boolean atEnd() {
                return index != 0;
            }

            @Override
            public @Nonnull Document getDocument() {
                return myEditor.getDocument();
            }

            @Override
            public IElementType getTokenType() {
                return IElementType.find(IElementType.FIRST_TOKEN_INDEX);
            }
        };
    }
}