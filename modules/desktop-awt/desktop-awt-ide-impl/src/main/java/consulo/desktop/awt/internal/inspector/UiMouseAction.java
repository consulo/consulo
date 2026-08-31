/*
 * Copyright 2000-2025 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.inspector;

import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.MouseShortcutPanel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareToggleAction;
import consulo.ui.ex.action.MouseShortcut;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;

public abstract class UiMouseAction extends DumbAwareToggleAction {
    private final String myActionId;
    private boolean myActive;

    public UiMouseAction(@Nonnull String actionId) {
        myActionId = actionId;
        setEnabledInModalContext(true);

        IdeEventQueue.getInstance().addDispatcher(this::handleEvent, null);
    }

    @Override
    public final boolean isSelected(AnActionEvent e) {
        return myActive;
    }

    @Override
    @RequiredUIAccess
    public final void setSelected(AnActionEvent e, boolean state) {
        if (myActive == state) {
            return;
        }
        myActive = state;
        onToggle(state);
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        if (ActionPlaces.MOUSE_SHORTCUT.equals(e.getPlace())) {
            e.getPresentation().setEnabled(false);
        }
    }

    private boolean handleEvent(AWTEvent event) {
        if (!myActive) {
            return false;
        }

        if (event instanceof MouseEvent mouseEvent && mouseEvent.getClickCount() > 0) {
            Component source = mouseEvent.getComponent();
            if (source == null) {
                return false;
            }

            Component component = UIUtil.getDeepestComponentAt(source, mouseEvent.getX(), mouseEvent.getY());
            if (component == null) {
                component = source;
            }
            if (component instanceof MouseShortcutPanel || component.getParent() instanceof MouseShortcutPanel) {
                return false;
            }

            MouseShortcut mouseShortcut = new MouseShortcut(mouseEvent.getButton(), mouseEvent.getModifiersEx(), mouseEvent.getClickCount());
            if (!hasShortcut(mouseShortcut)) {
                return false;
            }

            if (mouseEvent.getID() == MouseEvent.MOUSE_PRESSED) {
                handleClick(component, mouseEvent);
            }
            return true;
        }
        return false;
    }

    private boolean hasShortcut(MouseShortcut mouseShortcut) {
        Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
        for (Shortcut shortcut : keymap.getShortcuts(myActionId)) {
            if (mouseShortcut.equals(shortcut)) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    protected void onToggle(boolean state) {
    }

    @RequiredUIAccess
    protected abstract void handleClick(@Nonnull Component component, @Nullable MouseEvent event);
}
