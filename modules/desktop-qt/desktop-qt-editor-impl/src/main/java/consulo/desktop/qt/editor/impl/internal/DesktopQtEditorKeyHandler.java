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
package consulo.desktop.qt.editor.impl.internal;

import consulo.codeEditor.action.EditorActionManager;
import consulo.dataContext.DataContext;
import consulo.ui.UIAccess;
import consulo.desktop.qt.ui.impl.DesktopQtInputDetails;
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
import io.qt.gui.QKeyEvent;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

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

    private final DesktopQtEditorImpl myEditor;

    public DesktopQtEditorKeyHandler(DesktopQtEditorImpl editor) {
        myEditor = editor;
    }

    public boolean handle(QKeyEvent event) {
        EnumSet<Modifier> modifiers = DesktopQtInputDetails.modifiers(event.modifiers());

        // a printable character held down with nothing but shift is text, and asking the keymap about it first is
        // what turned shift and nine into a caret move - no shortcut is a bare character, so none is missed here
        if (isTypedCharacter(event, modifiers)) {
            return typeCharacter(event);
        }

        KeyCode keyCode = DesktopQtInputDetails.keyCode(event, modifiers);

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

}
