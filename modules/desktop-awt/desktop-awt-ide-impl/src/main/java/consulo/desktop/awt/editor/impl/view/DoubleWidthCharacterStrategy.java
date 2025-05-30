// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.editor.impl.view;

/**
 * A strategy to differentiate between single and double width characters.
 *
 * @see EditorView#setDoubleWidthCharacterStrategy(DoubleWidthCharacterStrategy)
 */
public interface DoubleWidthCharacterStrategy {
    /**
     * Determines whether a given character is a double-width one.
     *
     * @param codePoint the code point of the input character
     * @return {@code true} if the character is a double-width one, {@code false} otherwise
     */
    boolean isDoubleWidth(int codePoint);
}
