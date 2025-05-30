// CharacterGridImpl.java
// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.editor.impl.view;

import consulo.desktop.awt.editor.impl.DesktopEditorImpl;

import java.awt.*;

public class CharacterGridImpl implements CharacterGrid {
    private final DesktopEditorImpl editor;
    private DoubleWidthCharacterStrategy doubleWidthCharacterStrategy = codePoint -> false;

    public CharacterGridImpl(DesktopEditorImpl editor) {
        this.editor = editor;
    }

    private EditorView getView() {
        return editor.getView();
    }

    private float getColumnSpacing() {
        Float m = editor.getSettings().getCharacterGridWidthMultiplier();
        if (m == null) {
            throw new IllegalStateException("The editor must be in the grid mode to create an instance of a character grid");
        }
        return m;
    }

    private Dimension getSize() {
        return editor.getScrollingModel().getVisibleArea().getSize();
    }

    @Override
    public int getColumns() {
        int width = getSize().width;
        return width > 0 ? (int) (width / getCharWidth()) : 0;
    }

    @Override
    public int getRows() {
        int lineHeight = getView().getLineHeight();
        int height = getSize().height;
        return height > 0 ? (int) (height / lineHeight) : 0;
    }

    @Override
    public float getCharWidth() {
        return getView().getMaxCharWidth() * getColumnSpacing();
    }

    @Override
    public DoubleWidthCharacterStrategy getDoubleWidthCharacterStrategy() {
        return doubleWidthCharacterStrategy;
    }

    @Override
    public void setDoubleWidthCharacterStrategy(DoubleWidthCharacterStrategy strategy) {
        this.doubleWidthCharacterStrategy = strategy;
    }

    @Override
    public float codePointWidth(int codePoint) {
        return (doubleWidthCharacterStrategy.isDoubleWidth(codePoint) ? 2.0f : 1.0f) * getCharWidth();
    }

    @Override
    public CharacterGridIterator iterator(int startOffset, int endOffset) {
        return new CharacterGridIteratorImpl(editor.getDocument().getImmutableCharSequence(),
            doubleWidthCharacterStrategy, startOffset, endOffset);
    }
}
