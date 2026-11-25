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

import consulo.application.Application;
import consulo.application.internal.ApplicationEx;
import consulo.codeEditor.action.EditorActionManager;
import consulo.dataContext.DataContext;
import consulo.desktop.qt.ui.impl.DesktopQtInputDetails;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyStroke;
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

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Turns qt key presses into platform actions and typed characters.
 * <p>
 * Keys are described with {@link KeyCode} and {@link Modifier}, the platform's own input vocabulary, and looked up
 * in the {@link Keymap} as a {@link KeyStroke}. Nothing here hard-codes which key does what - the user's keymap
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
            UnifiedCustomShortcutDispatcher.collectKeymapActions(List.of(KeyStroke.of(keyCode, modifiers, false)), modalContext);
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
                    : application().runInputAsync(
                        uiAccess,
                        () -> UnifiedCustomShortcutDispatcher.performFirstEnabled(actions, myEditor.getDataContext(), uiEvent)
                    ),
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
        UIAccess uiAccess = UIAccess.current();
        if (ourStrokeQueue.isDone()) {
            ourStrokeQueue = application().runInput(uiAccess, () -> typeCharacter(ch));
            return true;
        }

        ourStrokeQueue = ourStrokeQueue
            .exceptionally(throwable -> null)
            .thenComposeAsync(
                ignored -> myEditor.isDisposed()
                    ? CompletableFuture.completedFuture(null)
                    : application().runInput(uiAccess, () -> typeCharacter(ch)).thenApply(result -> null),
                uiAccess
            );
        return true;
    }

    /**
     * True while keys typed before now have not run - they wait in the stroke queue, or for a background write action to let go
     * of write-intent.
     */
    static boolean isInputPending() {
        ApplicationEx application = application();
        return !application.isInsideInput() && (!ourStrokeQueue.isDone() || application.isInputPending());
    }

    /**
     * Runs a pointer action on the caret after the keys typed before it - moving the caret first would put them where the
     * pointer went.
     */
    static void runAfterPendingStrokes(UIAccess uiAccess, Runnable action) {
        if (!isInputPending()) {
            action.run();
            return;
        }

        ourStrokeQueue = ourStrokeQueue
            .exceptionally(throwable -> null)
            .thenComposeAsync(ignored -> application().runInput(uiAccess, action), uiAccess);
    }

    private static ApplicationEx application() {
        return (ApplicationEx) Application.get();
    }

    private void typeCharacter(char ch) {
        DataContext context = myEditor.getDataContext();

        ((ActionManagerEx) ActionManager.getInstance()).fireBeforeEditorTyping(ch, context);
        EditorActionManager.getInstance().getTypedAction().actionPerformed(myEditor, ch, context);
    }

}
