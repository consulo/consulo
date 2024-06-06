// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.action;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.ui.wm.ApplicationIdeFocusManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.ide.impl.idea.openapi.actionSystem.impl.WeakTimerListener;
import consulo.ide.impl.idea.openapi.keymap.ex.KeymapManagerEx;
import consulo.util.lang.Comparing;
import consulo.ui.ex.action.ActionButton;
import consulo.ui.ex.action.TimerListener;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.event.KeymapManagerListener;
import consulo.ui.ex.update.Activatable;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;

/**
 * @author Konstantin Bulenkov
 */
public abstract class ToolbarUpdater implements Activatable {
  private final ActionManagerEx myActionManager;
  private final KeymapManagerEx myKeymapManager;
  private final JComponent myComponent;

  private final KeymapManagerListener myKeymapManagerListener = new MyKeymapManagerListener();
  private final TimerListener myTimerListener = new MyTimerListener();
  private final WeakTimerListener myWeakTimerListener;

  private boolean myListenersArmed;

  public ToolbarUpdater(@Nonnull JComponent component) {
    this(KeymapManagerEx.getInstanceEx(), component);
  }

  public ToolbarUpdater(@Nonnull KeymapManagerEx keymapManager, @Nonnull JComponent component) {
    myActionManager = ActionManagerEx.getInstanceEx();
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
  public KeymapManagerEx getKeymapManager() {
    return myKeymapManager;
  }

  @Nonnull
  public ActionManagerEx getActionManager() {
    return myActionManager;
  }

  public void updateActions(boolean now, boolean forced) {
    updateActions(now, false, forced);
  }

  private void updateActions(boolean now, final boolean transparentOnly, final boolean forced) {
    final Runnable updateRunnable = new MyUpdateRunnable(this, transparentOnly, forced);
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

  protected abstract void updateActionsImpl(boolean transparentOnly, boolean forced);

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

      updateActions(false, myActionManager.isTransparentOnlyActionsUpdateNow(), false);
    }
  }

  private static class MyUpdateRunnable implements Runnable {
    private final boolean myTransparentOnly;
    private final boolean myForced;

    @Nonnull
    private final WeakReference<ToolbarUpdater> myUpdaterRef;
    private final int myHash;

    MyUpdateRunnable(@Nonnull ToolbarUpdater updater, boolean transparentOnly, boolean forced) {
      myTransparentOnly = transparentOnly;
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

      updater.updateActionsImpl(myTransparentOnly, myForced);
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
