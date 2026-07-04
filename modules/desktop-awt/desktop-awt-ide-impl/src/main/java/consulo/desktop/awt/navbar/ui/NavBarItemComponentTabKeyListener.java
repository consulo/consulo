// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.navbar.ui;

import javax.swing.JComponent;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Checks if the key event is a {@link KeyEvent#VK_TAB} or {@link KeyEvent#isShiftDown() shift} + {@link KeyEvent#VK_TAB} event,
 * consumes the event if so,
 * and moves the focus to next/previous component after/before the containing {@link NewNavBarPanel}.
 */
final class NavBarItemComponentTabKeyListener extends KeyAdapter {
    private final JComponent myPanel;

    NavBarItemComponentTabKeyListener(JComponent panel) {
        myPanel = panel;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_TAB && e.getSource() instanceof NavBarItemComponent) {
            e.consume();
            jumpToNextComponent(!e.isShiftDown());
        }
    }

    private void jumpToNextComponent(boolean next) {
        // The base will be first or last NavBarItemComponent in the NewNavBarPanel
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (next) {
            focusManager.focusNextComponent(myPanel.getComponent(myPanel.getComponentCount() - 1));
        }
        else {
            focusManager.focusPreviousComponent(myPanel.getComponent(0));
        }
    }
}
