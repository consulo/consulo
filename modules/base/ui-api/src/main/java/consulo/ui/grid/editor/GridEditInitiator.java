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
package consulo.ui.grid.editor;

/**
 * What started an edit, as {@link GridCellEditorFactory#createEditor} gets it - the same on every frontend.
 * <ul>
 * <li>{@link Kind#ACTION} - an action, Enter or F2. The editor starts with the formatted value.</li>
 * <li>{@link Kind#MOUSE} - a double click.</li>
 * <li>{@link Kind#KEY} - a typed key. {@link #typedText()} holds what was typed, and stands in for its key code (for example
 * {@code "t"} for the T key, {@code " "} for the space bar). A text editor starts empty; the frontend puts the typed text into
 * its field.</li>
 * </ul>
 *
 * @param typedText the typed text for {@link Kind#KEY}, otherwise empty
 * @since 2026-10-04
 */
public record GridEditInitiator(Kind kind, String typedText) {
    public enum Kind {
        ACTION,
        MOUSE,
        KEY
    }

    public static final GridEditInitiator ACTION = new GridEditInitiator(Kind.ACTION, "");
    public static final GridEditInitiator MOUSE = new GridEditInitiator(Kind.MOUSE, "");

    public static GridEditInitiator typed(String text) {
        return new GridEditInitiator(Kind.KEY, text);
    }

    public boolean isKey() {
        return kind == Kind.KEY;
    }

    /**
     * @return the first typed character in upper case, or {@code 0} when nothing was typed - the key code of a letter, a digit or
     * space
     */
    public char getTypedKeyChar() {
        return typedText.isEmpty() ? 0 : Character.toUpperCase(typedText.charAt(0));
    }
}
