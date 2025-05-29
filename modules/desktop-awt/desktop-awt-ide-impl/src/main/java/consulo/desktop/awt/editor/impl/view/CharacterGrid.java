// CharacterGrid.java
// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.editor.impl.view;

public interface CharacterGrid {
    int getColumns();

    int getRows();

    float getCharWidth();

    DoubleWidthCharacterStrategy getDoubleWidthCharacterStrategy();

    void setDoubleWidthCharacterStrategy(DoubleWidthCharacterStrategy strategy);

    float codePointWidth(int codePoint);

    CharacterGridIterator iterator(int startOffset, int endOffset);
}
