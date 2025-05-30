// CharacterGridIteratorImpl.java
// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.editor.impl.view;

class CharacterGridIteratorImpl implements CharacterGridIterator {
    private final CharSequence text;
    private final DoubleWidthCharacterStrategy doubleWidthCharacterStrategy;
    private final int startOffset;
    private final int endOffset;
    private final String substring;
    private final int textLength;
    private int codePoint;
    private boolean isAtEnd;
    private boolean isLineBreak;
    private int cellStartOffset;
    private int cellEndOffset;
    private int cellStartColumn;
    private int cellEndColumn;

    CharacterGridIteratorImpl(CharSequence text,
                              DoubleWidthCharacterStrategy doubleWidthCharacterStrategy,
                              int startOffset,
                              int endOffset) {
        this.text = text;
        this.doubleWidthCharacterStrategy = doubleWidthCharacterStrategy;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.substring = text.subSequence(startOffset, endOffset).toString();
        this.textLength = text.length();
        setOffset(startOffset);
        advanceColumn();
    }

    @Override
    public boolean isAtEnd() {
        return isAtEnd;
    }

    @Override
    public boolean isLineBreak() {
        return isLineBreak;
    }

    @Override
    public int getCellStartOffset() {
        return cellStartOffset;
    }

    @Override
    public int getCellEndOffset() {
        return cellEndOffset;
    }

    @Override
    public int getCellStartColumn() {
        return cellStartColumn;
    }

    @Override
    public int getCellEndColumn() {
        return cellEndColumn;
    }

    @Override
    public void advance() {
        setOffset(cellEndOffset);
        advanceColumn();
    }

    private void setOffset(int newOffset) {
        if (newOffset >= endOffset) {
            codePoint = 0;
            isAtEnd = true;
            isLineBreak = true;
            cellStartOffset = endOffset;
            cellEndOffset = endOffset;
        }
        else {
            int cp = substring.codePointAt(newOffset - startOffset);
            codePoint = cp;
            isAtEnd = false;
            isLineBreak = (cp == '\n' || cp == '\r');
            boolean bmp = Character.isBmpCodePoint(cp);
            cellStartOffset = newOffset;
            if (bmp) {
                if (cp == '\r' && newOffset + 1 < textLength &&
                    substring.charAt(newOffset - startOffset + 1) == '\n') {
                    cellEndOffset = newOffset + 2;
                }
                else {
                    cellEndOffset = newOffset + 1;
                }
            }
            else {
                cellEndOffset = newOffset + 2;
            }
        }
    }

    private void advanceColumn() {
        cellStartColumn = cellEndColumn;
        if (isLineBreak) {
            cellEndColumn = 0;
        }
        else {
            cellEndColumn = cellStartColumn + (doubleWidthCharacterStrategy.isDoubleWidth(codePoint) ? 2 : 1);
        }
    }
}
