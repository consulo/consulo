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
package consulo.desktop.qt.editor.impl;

import consulo.codeEditor.action.EditorActionManager;
import consulo.dataContext.DataContext;
import consulo.ui.UIAccess;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.ModifiedInputDetails.Modifier;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.impl.internal.action.ActionRunnerAsync;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import io.qt.core.Qt;
import io.qt.gui.QKeyEvent;
import org.jspecify.annotations.Nullable;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Turns qt key presses into platform actions and typed characters.
 * <p>
 * Keys are described with {@link KeyCode} and {@link Modifier}, the platform's own input vocabulary, rather than
 * with {@code java.awt.event.KeyEvent}. The one place awt still appears is the lookup itself: {@link Keymap} is
 * keyed by {@link KeyStroke} and offers no other way in, so the neutral description is converted to a keystroke at
 * that boundary and nowhere else. That is also why nothing here hard-codes which key does what - the user's keymap
 * decides, exactly as it does for the awt frontend.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorKeyHandler {
    /**
     * Qt numbers letters, digits and space by their character code, which is what the key codes of those keys are
     * too. Every other key qt and the platform number differently and has to be named here.
     * <p>
     * Punctuation is deliberately absent. Qt reports the character the key produces, so shift and nine arrive as
     * {@code Key_ParenLeft}, whose code collides with the code of the down arrow - which is how typing a bracket
     * used to move the caret instead. Punctuation reaches the keymap only through {@link #shiftedKeyCode}, which
     * works from the unshifted character rather than from the qt key.
     */
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

    private final DesktopQtEditorImpl myEditor;

    public DesktopQtEditorKeyHandler(DesktopQtEditorImpl editor) {
        myEditor = editor;
    }

    public boolean handle(QKeyEvent event) {
        EnumSet<Modifier> modifiers = toModifiers(event.modifiers());

        // a printable character held down with nothing but shift is text, and asking the keymap about it first is
        // what turned shift and nine into a caret move - no shortcut is a bare character, so none is missed here
        if (isTypedCharacter(event, modifiers)) {
            return typeCharacter(event);
        }

        KeyCode keyCode = toKeyCode(event, modifiers);

        return keyCode != null && performKeymapAction(keyCode, modifiers) || typeCharacter(event);
    }

    private boolean performKeymapAction(KeyCode keyCode, EnumSet<Modifier> modifiers) {
        Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
        if (keymap == null) {
            return false;
        }

        ActionManager actionManager = ActionManager.getInstance();

        List<AnAction> actions = new ArrayList<>();
        for (String actionId : keymap.getActionIds(toKeyStroke(keyCode, modifiers))) {
            AnAction action = actionManager.getAction(actionId);
            if (action != null) {
                actions.add(action);
            }
        }

        if (actions.isEmpty()) {
            return false;
        }

        // the order the keymap answers in says nothing about which action the stroke belongs to - the weight does,
        // the same way IdeKeyEventDispatcher sorts before offering the stroke to anything
        actions.sort(Comparator.comparing(AnAction::getExecuteWeight).reversed());

        // whether an action is enabled is only answerable asynchronously, but the key has to be claimed or dropped
        // now - so a stroke the keymap knows is claimed, and the search for a live action for it continues after
        performFirstEnabled(actions, 0, myEditor.getDataContext(), UIAccess.current());
        return true;
    }

    private void performFirstEnabled(List<AnAction> actions, int index, DataContext context, UIAccess uiAccess) {
        if (index >= actions.size()) {
            return;
        }

        ActionManagerEx actionManager = (ActionManagerEx) ActionManager.getInstance();

        AnAction action = actions.get(index);
        AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.KEYBOARD_SHORTCUT, context);

        ActionRunnerAsync.lastUpdateAndCheckDumbAsync(action, event, false).whenCompleteAsync((enabled, throwable) -> {
            if (throwable != null || !Boolean.TRUE.equals(enabled)) {
                performFirstEnabled(actions, index + 1, context, uiAccess);
                return;
            }

            actionManager.fireBeforeActionPerformed(action, context, event);
            actionManager.performActionDumbAware(action, event);
            actionManager.queueActionPerformedEvent(action, context, event);
        }, uiAccess);
    }

    private boolean isTypedCharacter(QKeyEvent event, EnumSet<Modifier> modifiers) {
        if (modifiers.contains(Modifier.CTRL) || modifiers.contains(Modifier.ALT) || modifiers.contains(Modifier.META)) {
            return false;
        }

        String text = event.text();
        return text.length() == 1 && !Character.isISOControl(text.charAt(0));
    }

    private boolean typeCharacter(QKeyEvent event) {
        if (myEditor.isViewer() || !myEditor.getDocument().isWritable()) {
            return false;
        }

        String text = event.text();
        if (text.length() != 1 || Character.isISOControl(text.charAt(0))) {
            return false;
        }

        EditorActionManager.getInstance().getTypedAction().actionPerformed(myEditor, text.charAt(0), myEditor.getDataContext());
        return true;
    }

    private static @Nullable KeyCode toKeyCode(QKeyEvent event, EnumSet<Modifier> modifiers) {
        KeyCode named = QT_KEY_CODES.get(event.key());
        if (named != null) {
            return named;
        }

        int key = event.key();
        if (key >= 'A' && key <= 'Z' || key >= '0' && key <= '9' || key == ' ') {
            return KeyCode.of(key);
        }

        return modifiers.contains(Modifier.SHIFT) ? shiftedKeyCode(event) : null;
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

    private static KeyStroke toKeyStroke(KeyCode keyCode, EnumSet<Modifier> modifiers) {
        int awtModifiers = 0;
        if (modifiers.contains(Modifier.SHIFT)) {
            awtModifiers |= InputEvent.SHIFT_DOWN_MASK;
        }
        if (modifiers.contains(Modifier.CTRL)) {
            awtModifiers |= InputEvent.CTRL_DOWN_MASK;
        }
        if (modifiers.contains(Modifier.ALT)) {
            awtModifiers |= InputEvent.ALT_DOWN_MASK;
        }
        if (modifiers.contains(Modifier.META)) {
            awtModifiers |= InputEvent.META_DOWN_MASK;
        }

        return KeyStroke.getKeyStroke(keyCode.key(), awtModifiers);
    }

    private static EnumSet<Modifier> toModifiers(Qt.KeyboardModifiers modifiers) {
        EnumSet<Modifier> result = EnumSet.noneOf(Modifier.class);
        if (modifiers.testFlag(Qt.KeyboardModifier.ShiftModifier)) {
            result.add(Modifier.SHIFT);
        }
        if (modifiers.testFlag(Qt.KeyboardModifier.ControlModifier)) {
            result.add(Modifier.CTRL);
        }
        if (modifiers.testFlag(Qt.KeyboardModifier.AltModifier)) {
            result.add(Modifier.ALT);
        }
        if (modifiers.testFlag(Qt.KeyboardModifier.MetaModifier)) {
            result.add(Modifier.META);
        }
        return result;
    }
}
