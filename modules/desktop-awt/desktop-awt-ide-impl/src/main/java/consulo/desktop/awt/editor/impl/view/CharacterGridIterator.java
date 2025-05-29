// CharacterGridIterator.java
// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.editor.impl.view;

public interface CharacterGridIterator {
    boolean isAtEnd();

    boolean isLineBreak();

    int getCellStartOffset();

    int getCellEndOffset();

    int getCellStartColumn();

    int getCellEndColumn();

    void advance();
}
