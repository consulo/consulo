// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.action.toolbar;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.ui.wm.ApplicationIdeFocusManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.ide.impl.idea.openapi.actionSystem.impl.WeakTimerListener;
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
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;

/**
 * @author Konstantin Bulenkov
 */
public abstract class ToolbarUpdater implements Activatable {
  private final ActionManagerEx myActionManager;
  private final KeymapManager myKeymapManager;
  private final JComponent myComponent;

  private final KeymapManagerListener myKeymapManagerListener = new MyKeymapManagerListener();
  private final TimerListener myTimerListener = new MyTimerListener();
  private final WeakTimerListener myWeakTimerListener;

  private boolean myListenersArmed;

  public ToolbarUpdater(@Nonnull KeymapManager keymapManager,
                        @Nonnull ActionManager actionManager,
                        @Nonnull JComponent component) {
    myActionManager = (ActionManagerEx) actionManager;
    myKeymapManager = keymapManager;
    myComponent = component;
    myWeakTimerListener = new WeakTimerListener(myTimerListener);
    new UiNotifyConnector(component, this);
  }

  @Override
  public void showNotify() {
    if (myListenersArmed) return;
    myListenersArmed = true;
    myActionManager.addTimerListener(500, myWeakTimerListener);
    myActionManager.addTransparentTimerListener(500, myWeakTimerListener);
    myKeymapManager.addKeymapManagerListener(myKeymapManagerListener);
    updateActionTooltips();
  }

  @Override
  public void hideNotify() {
    if (!myListenersArmed) return;
    myListenersArmed = false;
    myActionManager.removeTimerListener(myWeakTimerListener);
    myActionManager.removeTransparentTimerListener(myWeakTimerListener);
    myKeymapManager.removeKeymapManagerListener(myKeymapManagerListener);
  }

  @Nonnull
  public KeymapManager getKeymapManager() {
    return myKeymapManager;
  }

  @Nonnull
  public ActionManagerEx getActionManager() {
    return myActionManager;
  }

  public void updateActions(boolean now, final boolean forced) {
    final Runnable updateRunnable = new MyUpdateRunnable(this, forced);
    final Application app = ApplicationManager.getApplication();

    if (now || (app.isUnitTestMode() && app.isDispatchThread())) {
      updateRunnable.run();
    }
    else {
      final IdeFocusManager fm = ApplicationIdeFocusManager.getInstance();

      if (!app.isHeadlessEnvironment()) {
        if (app.isDispatchThread() && myComponent.isShowing()) {
          fm.doWhenFocusSettlesDown(updateRunnable);
        }
        else {
          UiNotifyConnector.doWhenFirstShown(myComponent, () -> fm.doWhenFocusSettlesDown(updateRunnable));
        }
      }
    }
  }

  protected abstract void updateActionsImpl(boolean forced);

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
    public IdeaModalityState getModalityState() {
      return IdeaModalityState.stateForComponent(myComponent);
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
      if (window instanceof Dialog && ((Dialog)window).isModal() && !SwingUtilities.isDescendingFrom(myComponent, window)) {
        return;
      }

      updateActions(false, false);
    }
  }

  private static class MyUpdateRunnable implements Runnable {
    private final boolean myForced;

    @Nonnull
    private final WeakReference<ToolbarUpdater> myUpdaterRef;
    private final int myHash;

    MyUpdateRunnable(@Nonnull ToolbarUpdater updater, boolean forced) {
      myForced = forced;
      myHash = updater.hashCode();

      myUpdaterRef = new WeakReference<>(updater);
    }

    @Override
    public void run() {
      ToolbarUpdater updater = myUpdaterRef.get();
      if (updater == null) return;

      if (!updater.myComponent.isVisible() && !ApplicationManager.getApplication().isUnitTestMode()) {
        return;
      }

      updater.updateActionsImpl(myForced);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof MyUpdateRunnable)) return false;

      MyUpdateRunnable that = (MyUpdateRunnable)obj;
      if (myHash != that.myHash) return false;

      ToolbarUpdater updater1 = myUpdaterRef.get();
      ToolbarUpdater updater2 = that.myUpdaterRef.get();
      return Comparing.equal(updater1, updater2);
    }

    @Override
    public int hashCode() {
      return myHash;
    }
  }
}
