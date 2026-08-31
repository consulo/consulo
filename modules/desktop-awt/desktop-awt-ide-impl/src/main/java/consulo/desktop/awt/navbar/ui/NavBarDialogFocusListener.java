// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.navbar.ui;

import consulo.disposer.Disposer;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.UIUtil;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Consider the following steps:
 * <ul>
 * <li>navigation bar is focused;</li>
 * <li>user invokes Delete action;</li>
 * <li>Delete dialog is open;</li>
 * <li>user completes the dialog with OK;</li>
 * <li>dialog is closed, element is removed;</li>
 * <li>focus goes back to navigation bar.</li>
 * </ul>
 * Since the navigation bar is focused, no updates from focus data context are applied.
 * This listener detects the above situation, and moves the focus back to the editor.
 */
final class NavBarDialogFocusListener implements FocusListener {
    private final NewNavBarPanel myPanel;

    private boolean myShouldFocusEditor = false;

    NavBarDialogFocusListener(NewNavBarPanel panel) {
        myPanel = panel;
    }

    @Override
    public void focusGained(FocusEvent e) {
        // If focus comes from anything in the nav bar panel, ignore the event
        if (NavBarItemComponent.isItemComponentFocusable() && UIUtil.isAncestor(myPanel, e.getOppositeComponent())) {
            return;
        }

        if (e.getOppositeComponent() == null) {
            if (myShouldFocusEditor) {
                myShouldFocusEditor = false;
                ToolWindowManager.getInstance(myPanel.getProject()).activateEditorComponent();
            }
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        // If focus reaches anything in nav bar panel, ignore the event
        if (NavBarItemComponent.isItemComponentFocusable() && UIUtil.isAncestor(myPanel, e.getOppositeComponent())) {
            return;
        }

        DialogWrapper dialog = DialogWrapper.findInstance(e.getOppositeComponent());
        if (dialog != null) {
            if (dialog.isDisposed()) {
                myShouldFocusEditor = dialog.getExitCode() != DialogWrapper.CANCEL_EXIT_CODE;
            }
            else {
                myShouldFocusEditor = true;
                Disposer.register(dialog.getDisposable(), () -> {
                    if (dialog.getExitCode() == DialogWrapper.CANCEL_EXIT_CODE) {
                        myShouldFocusEditor = false;
                    }
                });
            }
        }
    }
}
