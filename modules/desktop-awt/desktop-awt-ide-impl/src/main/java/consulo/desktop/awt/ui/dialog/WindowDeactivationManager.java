// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.ui.dialog;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.wm.WindowManager;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

public final class WindowDeactivationManager {
    private WindowDeactivationManager() {
    }

    public static void addWindowDeactivationListener(
        Window window,
        @Nullable Project project,
        Disposable disposable,
        Runnable onWindowDeactivated
    ) {
        WindowFocusListener focusListener = new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                onWindowDeactivated.run();
            }
        };

        window.addWindowListener(new WindowAdapter() {
            private boolean myWasOpened;

            @Override
            public void windowDeactivated(WindowEvent e) {
                if (!myWasOpened) {
                    return;
                }
                // At the moment of deactivation there is just "temporary" focus owner (main frame),
                // true focus owner (Search Everywhere popup etc.) appears later so the check should be postponed too
                Application application = Application.get();
                application.invokeLater(
                    () -> {
                        Component focusOwner = ProjectIdeFocusManager.getInstance(project).getFocusOwner();
                        if (focusOwner == null) {
                            return;
                        }
                        if (!SwingUtilities.isDescendingFrom(focusOwner, window)) {
                            onWindowDeactivated.run();
                        }
                    },
                    application.getCurrentModalityState()
                );
            }

            @Override
            public void windowOpened(WindowEvent e) {
                // Currently, in WSLg environment dialog showing generates focus events corresponding to dialog getting focus,
                // then losing it to main frame, then getting it again immediately. As a workaround, we track focus/activation events only after
                // 'window opened' event is received.
                // Another case when such delaying can make sense is when the dialog is showing at the same time some popup is closing
                // (e.g. invoking 'Find in Files...' from a quick list).
                myWasOpened = true;

                JFrame frame = WindowManager.getInstance().getFrame(project);
                if (frame != null && Disposer.tryRegister(disposable, () -> frame.removeWindowFocusListener(focusListener))) {
                    frame.addWindowFocusListener(focusListener);
                }
            }
        });
    }
}
