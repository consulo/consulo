// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.action.toolbar;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ui.wm.ApplicationIdeFocusManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.ide.impl.idea.openapi.actionSystem.impl.WeakTimerListener;
import consulo.ui.ModalityState;
import consulo.ui.ex.action.ActionButton;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.TimerListener;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.keymap.event.KeymapManagerListener;
import consulo.ui.ex.update.Activatable;
import consulo.util.lang.Comparing;
import consulo.util.lang.ThreeState;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;

/**
 * @author Konstantin Bulenkov
 */
public class ToolbarNotifier implements Activatable {
    private final ActionManagerEx myActionManager;
    private final KeymapManager myKeymapManager;
    private final JComponent myComponent;

    private final KeymapManagerListener myKeymapManagerListener = new MyKeymapManagerListener();
    private final WeakTimerListener myWeakTimerListener;

    @Nonnull
    private final Application myApplication;

    @Nonnull
    private final Runnable myUpdateActionsNotifier;

    @SuppressWarnings("FieldCanBeLocal") // do not inline creating - it will removed from memory due WeakTimerListener
    private MyTimerListener myTimerListener = new MyTimerListener();

    private boolean myListenersArmed;

    public ToolbarNotifier(@Nonnull Application application,
                           @Nonnull KeymapManager keymapManager,
                           @Nonnull ActionManager actionManager,
                           @Nonnull JComponent component,
                           @Nonnull Runnable updateActionsNotifier) {
        myApplication = application;
        myUpdateActionsNotifier = updateActionsNotifier;
        myActionManager = (ActionManagerEx) actionManager;
        myKeymapManager = keymapManager;
        myComponent = component;
        myWeakTimerListener = new WeakTimerListener(myTimerListener);

        new UiNotifyConnector(component, this);
    }

    @Override
    public void showNotify() {
        if (myListenersArmed) {
            return;
        }
        myListenersArmed = true;
        myActionManager.addTimerListener(500, myWeakTimerListener);
        myActionManager.addTransparentTimerListener(500, myWeakTimerListener);
        myKeymapManager.addKeymapManagerListener(myKeymapManagerListener);
        updateActionTooltips();
    }

    @Override
    public void hideNotify() {
        if (!myListenersArmed) {
            return;
        }
        myListenersArmed = false;
        myActionManager.removeTimerListener(myWeakTimerListener);
        myActionManager.removeTransparentTimerListener(myWeakTimerListener);
        myKeymapManager.removeKeymapManagerListener(myKeymapManagerListener);
    }

    public void updateActions(boolean now) {
        Runnable updateRunnable = new MyUpdateRunnable(this);

        if (now) {
            updateRunnable.run();
        }
        else {
            IdeFocusManager fm = ApplicationIdeFocusManager.getInstance();

            if (myApplication.isDispatchThread() && myComponent.isShowing()) {
                fm.doWhenFocusSettlesDown(updateRunnable);
            }
            else {
                UiNotifyConnector.doWhenFirstShown(myComponent, () -> fm.doWhenFocusSettlesDown(updateRunnable));
            }
        }
    }

    protected void updateActionsImpl() {
        if (myApplication.getDisposeState().get() == ThreeState.NO) {
            myUpdateActionsNotifier.run();
        }
    }

    protected void updateActionTooltips() {
        for (ActionButton actionButton : UIUtil.uiTraverser(myComponent).preOrderDfsTraversal().filter(ActionButton.class)) {
            actionButton.updateToolTipText();
        }
    }

    private final class MyKeymapManagerListener implements KeymapManagerListener {
        @Override
        public void activeKeymapChanged(Keymap keymap) {
            updateActionTooltips();
        }
    }

    private final class MyTimerListener implements TimerListener {

        @Override
        public ModalityState getModalityState() {
            return myApplication.getModalityStateForComponent(myComponent);
        }

        @Override
        public void run() {
            if (!myComponent.isShowing()) {
                return;
            }

            // do not update when a popup menu is shown (if popup menu contains action which is also in the toolbar, it should not be enabled/disabled)
            MenuSelectionManager menuSelectionManager = MenuSelectionManager.defaultManager();
            MenuElement[] selectedPath = menuSelectionManager.getSelectedPath();
            if (selectedPath.length > 0) {
                return;
            }

            // don't update toolbar if there is currently active modal dialog
            Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
            if (window instanceof Dialog && ((Dialog) window).isModal() && !SwingUtilities.isDescendingFrom(myComponent, window)) {
                return;
            }

            updateActions(false);
        }
    }

    private static class MyUpdateRunnable implements Runnable {
        @Nonnull
        private final WeakReference<ToolbarNotifier> myUpdaterRef;
        private final int myHash;

        MyUpdateRunnable(@Nonnull ToolbarNotifier updater) {
            myHash = updater.hashCode();
            myUpdaterRef = new WeakReference<>(updater);
        }

        @Override
        public void run() {
            ToolbarNotifier updater = myUpdaterRef.get();
            if (updater == null) {
                return;
            }

            if (!updater.myComponent.isVisible() && !ApplicationManager.getApplication().isUnitTestMode()) {
                return;
            }

            updater.updateActionsImpl();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MyUpdateRunnable)) {
                return false;
            }

            MyUpdateRunnable that = (MyUpdateRunnable) obj;
            if (myHash != that.myHash) {
                return false;
            }

            ToolbarNotifier updater1 = myUpdaterRef.get();
            ToolbarNotifier updater2 = that.myUpdaterRef.get();
            return Comparing.equal(updater1, updater2);
        }

        @Override
        public int hashCode() {
            return myHash;
        }
    }
}
