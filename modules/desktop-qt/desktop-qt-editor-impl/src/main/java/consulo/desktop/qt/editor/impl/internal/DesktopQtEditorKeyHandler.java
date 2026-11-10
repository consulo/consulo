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
import consulo.desktop.qt.ui.impl.DesktopQtInputDetails;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.ModifiedInputDetails.Modifier;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.impl.internal.action.UnifiedCustomShortcutDispatcher;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.keymap.Keymap;
import io.qt.core.QCoreApplication;
import io.qt.gui.QKeyEvent;
import io.qt.widgets.QApplication;
import io.qt.widgets.QWidget;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    private static CompletableFuture<?> ourStrokeQueue = CompletableFuture.completedFuture(null);

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

        return keyCode != null && performKeymapAction(event, keyCode, modifiers) || typeCharacter(event);
    }

    private boolean performKeymapAction(QKeyEvent keyEvent, KeyCode keyCode, EnumSet<Modifier> modifiers) {
        boolean modalContext = QApplication.activeModalWidget() != null;
        List<AnAction> actions =
            UnifiedCustomShortcutDispatcher.collectKeymapActions(List.of(toKeyStroke(keyCode, modifiers)), modalContext);
        if (actions.isEmpty()) {
            return false;
        }

        // whether an action is enabled is only answerable asynchronously, but the key has to be claimed or dropped
        // now - so a stroke the keymap knows is claimed, and the search for a live action for it continues after
        QKeyEvent copy = keyEvent.clone();
        ComponentEvent<Component> uiEvent =
            new ComponentEvent<>(myEditor.getContentUIComponent(), DesktopQtInputDetails.keyboard(myEditor.getSurface(), keyEvent));
        UIAccess uiAccess = UIAccess.current();
        ourStrokeQueue = ourStrokeQueue
            .exceptionally(throwable -> null)
            .thenComposeAsync(
                ignored -> myEditor.isDisposed()
                    ? CompletableFuture.completedFuture(Boolean.TRUE)
                    : UnifiedCustomShortcutDispatcher.performFirstEnabled(actions, myEditor.getDataContext(), uiEvent),
                uiAccess
            )
            .whenCompleteAsync((performed, throwable) -> {
                if (!Boolean.TRUE.equals(performed) && !myEditor.isDisposed()) {
                    myEditor.fireKeyNotConsumed(keyCode);
                    passToParent(copy);
                }
            }, uiAccess);
        return true;
    }

    private void passToParent(QKeyEvent keyEvent) {
        DesktopQtEditorWidget surface = myEditor.getSurface();
        QWidget parent = surface == null || surface.isDisposed() ? null : surface.parentWidget();
        if (parent != null && !parent.isDisposed()) {
            QCoreApplication.sendEvent(parent, keyEvent);
        }
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

        char ch = text.charAt(0);
        if (ourStrokeQueue.isDone()) {
            typeCharacter(ch);
            return true;
        }

        UIAccess uiAccess = UIAccess.current();
        ourStrokeQueue = ourStrokeQueue
            .exceptionally(throwable -> null)
            .thenRunAsync(() -> {
                if (!myEditor.isDisposed()) {
                    typeCharacter(ch);
                }
            }, uiAccess);
        return true;
    }

    private void typeCharacter(char ch) {
        DataContext context = myEditor.getDataContext();

        ((ActionManagerEx) ActionManager.getInstance()).fireBeforeEditorTyping(ch, context);
        EditorActionManager.getInstance().getTypedAction().actionPerformed(myEditor, ch, context);
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
