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
package consulo.desktop.qt.ui.impl;

import consulo.ui.Point2D;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.event.details.MouseInputDetails;
import io.qt.core.QPoint;
import io.qt.core.QPointF;
import io.qt.core.Qt;
import io.qt.gui.QCursor;
import io.qt.gui.QGuiApplication;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QMouseEvent;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Map;

/**
 * The signals a click is dispatched from - {@code QAbstractButton.clicked}, {@code QAction.triggered} - carry no
 * position, and that is what a caller placing a popup at the click needs. Where the widget kept the
 * {@link QMouseEvent} which drove the signal it is asked for the position, and otherwise the pointer itself is,
 * which is the same place the click came from as long as it came from a pointer at all.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtInputDetails {
    private static final Map<Integer, KeyCode> QT_KEY_CODES = Map.ofEntries(
        Map.entry(Qt.Key.Key_Escape.value(), KeyCode.ESCAPE),
        Map.entry(Qt.Key.Key_Tab.value(), KeyCode.TAB),
        Map.entry(Qt.Key.Key_Backtab.value(), KeyCode.TAB),
        Map.entry(Qt.Key.Key_Return.value(), KeyCode.ENTER),
        Map.entry(Qt.Key.Key_Enter.value(), KeyCode.ENTER),
        Map.entry(Qt.Key.Key_Home.value(), KeyCode.HOME),
        Map.entry(Qt.Key.Key_End.value(), KeyCode.END),
        Map.entry(Qt.Key.Key_Left.value(), KeyCode.LEFT),
        Map.entry(Qt.Key.Key_Up.value(), KeyCode.UP),
        Map.entry(Qt.Key.Key_Right.value(), KeyCode.RIGHT),
        Map.entry(Qt.Key.Key_Down.value(), KeyCode.DOWN),
        Map.entry(Qt.Key.Key_Backspace.value(), KeyCode.of(0x08, "VK_BACK_SPACE")),
        Map.entry(Qt.Key.Key_Delete.value(), KeyCode.of(0x7F, "VK_DELETE")),
        Map.entry(Qt.Key.Key_Insert.value(), KeyCode.of(0x9B, "VK_INSERT")),
        Map.entry(Qt.Key.Key_PageUp.value(), KeyCode.of(0x21, "VK_PAGE_UP")),
        Map.entry(Qt.Key.Key_PageDown.value(), KeyCode.of(0x22, "VK_PAGE_DOWN"))
    );

    /**
     * The details of a key press, the keyboard counterpart of {@link #mouse}. Without it a qt widget can report
     * that a key was pressed but not which one, and the platform reads the key off the details - the new item
     * popup and run anything both do.
     */
    public static KeyboardInputDetails keyboard(@Nullable QWidget widget, QKeyEvent event) {
        QPoint onScreen = QCursor.pos();
        QPoint position = widget != null && !widget.isDisposed() ? widget.mapFromGlobal(onScreen) : onScreen;

        EnumSet<ModifiedInputDetails.Modifier> modifiers = modifiers(event.modifiers());

        return new KeyboardInputDetails(
            new Point2D(position.x(), position.y()),
            new Point2D(onScreen.x(), onScreen.y()),
            modifiers,
            keyCode(event, modifiers)
        );
    }

    /**
     * The platform's name for the key that was pressed, or null for one it has no name for.
     * <p>
     * Qt numbers letters, digits and space by their character code, which is what the key codes of those keys are
     * too. Every other key qt and the platform number differently and has to be named in {@link #QT_KEY_CODES}.
     * <p>
     * Punctuation is deliberately absent from that table. Qt reports the character the key produces, so shift and
     * nine arrive as {@code Key_ParenLeft}, whose code collides with the code of the down arrow - which is how
     * typing a bracket once moved the caret instead. Punctuation is reached through {@link #shiftedKeyCode},
     * which works from the unshifted character rather than from the qt key.
     */
    public static @Nullable KeyCode keyCode(QKeyEvent event, EnumSet<ModifiedInputDetails.Modifier> modifiers) {
        KeyCode named = QT_KEY_CODES.get(event.key());
        if (named != null) {
            return named;
        }

        int key = event.key();
        if (key >= 'A' && key <= 'Z' || key >= '0' && key <= '9' || key == ' ') {
            return KeyCode.of(key);
        }

        return modifiers.contains(ModifiedInputDetails.Modifier.SHIFT) ? shiftedKeyCode(event) : null;
    }

    /**
     * The key a shifted punctuation character was produced by. A shortcut is bound to the key, not to the character
     * on it - ctrl shift slash is written that way even though the key press carries a question mark.
     */
    private static @Nullable KeyCode shiftedKeyCode(QKeyEvent event) {
        int unshifted = switch (event.key()) {
            case 0x21 -> '1'; // Key_Exclam
            case 0x40 -> '2'; // Key_At
            case 0x23 -> '3'; // Key_NumberSign
            case 0x24 -> '4'; // Key_Dollar
            case 0x25 -> '5'; // Key_Percent
            case 0x5E -> '6'; // Key_AsciiCircum
            case 0x26 -> '7'; // Key_Ampersand
            case 0x2A -> '8'; // Key_Asterisk
            case 0x28 -> '9'; // Key_ParenLeft
            case 0x29 -> '0'; // Key_ParenRight
            case 0x3F -> '/'; // Key_Question
            case 0x3A -> ';'; // Key_Colon
            case 0x2B -> '='; // Key_Plus
            case 0x5F -> '-'; // Key_Underscore
            case 0x3C -> ','; // Key_Less
            case 0x3E -> '.'; // Key_Greater
            default -> -1;
        };

        return unshifted < 0 ? null : KeyCode.of(unshifted);
    }
    /**
     * The details of a click for which the mouse event which drove it is at hand.
     */
    public static MouseInputDetails mouse(@Nullable QWidget widget, QMouseEvent event) {
        return mouse(widget, event.globalPosition(), event.button(), event.modifiers());
    }

    /**
     * The details of a click which carries no event of its own - the pointer stands in for it.
     */
    public static MouseInputDetails mouseAtCursor(@Nullable QWidget widget) {
        return mouse(widget, new QPointF(QCursor.pos()), pressedButton(), QGuiApplication.keyboardModifiers());
    }

    public static MouseInputDetails mouse(
        @Nullable QWidget widget,
        QPointF globalPosition,
        Qt.MouseButton button,
        Qt.KeyboardModifiers modifiers
    ) {
        QPoint onScreen = globalPosition.toPoint();
        QPoint position = widget != null && !widget.isDisposed() ? widget.mapFromGlobal(onScreen) : onScreen;

        return new MouseInputDetails(
            new Point2D(position.x(), position.y()),
            new Point2D(onScreen.x(), onScreen.y()),
            modifiers(modifiers),
            button(button)
        );
    }

    /**
     * A {@code clicked} signal is emitted after the button was let go of, so nothing is held down by then and the
     * left button - the one every control answers to - is what stands for the click.
     */
    private static Qt.MouseButton pressedButton() {
        for (Qt.MouseButton button : QGuiApplication.mouseButtons().flags()) {
            if (button != Qt.MouseButton.NoButton) {
                return button;
            }
        }

        return Qt.MouseButton.LeftButton;
    }

    public static EnumSet<ModifiedInputDetails.Modifier> modifiers(Qt.KeyboardModifiers modifiers) {
        EnumSet<ModifiedInputDetails.Modifier> result = EnumSet.noneOf(ModifiedInputDetails.Modifier.class);

        if (modifiers.testFlag(Qt.KeyboardModifier.AltModifier)) {
            result.add(ModifiedInputDetails.Modifier.ALT);
        }
        if (modifiers.testFlag(Qt.KeyboardModifier.ControlModifier)) {
            result.add(ModifiedInputDetails.Modifier.CTRL);
        }
        if (modifiers.testFlag(Qt.KeyboardModifier.ShiftModifier)) {
            result.add(ModifiedInputDetails.Modifier.SHIFT);
        }
        if (modifiers.testFlag(Qt.KeyboardModifier.MetaModifier)) {
            result.add(ModifiedInputDetails.Modifier.META);
        }

        return result;
    }

    public static MouseInputDetails.MouseButton button(Qt.MouseButton button) {
        return switch (button) {
            case MiddleButton -> MouseInputDetails.MouseButton.MIDDLE;
            case RightButton -> MouseInputDetails.MouseButton.RIGHT;
            default -> MouseInputDetails.MouseButton.LEFT;
        };
    }
}
