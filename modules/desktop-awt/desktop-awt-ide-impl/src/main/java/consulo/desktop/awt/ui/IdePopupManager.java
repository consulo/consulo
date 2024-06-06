// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui;

import consulo.application.util.registry.Registry;
import consulo.desktop.awt.ui.keymap.IdeKeyEventDispatcher;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.IdeFrameUtil;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.IdePopupEventDispatcher;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.collection.Lists;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.Predicate;

public final class IdePopupManager implements Predicate<AWTEvent> {
  private static final Logger LOG = Logger.getInstance(IdePopupManager.class);

  private final List<IdePopupEventDispatcher> myDispatchStack = Lists.newLockFreeCopyOnWriteList();
  private boolean myIgnoreNextKeyTypedEvent;

  boolean isPopupActive() {
    for (IdePopupEventDispatcher each : myDispatchStack) {
      if (each.getComponent() == null || !each.getComponent().isShowing()) {
        myDispatchStack.remove(each);
      }
    }

    return !myDispatchStack.isEmpty();
  }

  @Override
  public boolean test(@Nonnull final AWTEvent e) {
    LOG.assertTrue(isPopupActive());

    if (e.getID() == WindowEvent.WINDOW_LOST_FOCUS || e.getID() == WindowEvent.WINDOW_DEACTIVATED) {
      if (!isPopupActive()) return false;

      Window focused = ((WindowEvent)e).getOppositeWindow();
      if (focused == null) {
        focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
      }

      Component ultimateParentForFocusedComponent = UIUtil.findUltimateParent(focused);
      Window sourceWindow = ((WindowEvent)e).getWindow();
      Component ultimateParentForEventWindow = UIUtil.findUltimateParent(sourceWindow);

      boolean shouldCloseAllPopup = false;
      if (ultimateParentForEventWindow == null || ultimateParentForFocusedComponent == null) {
        shouldCloseAllPopup = true;
      }

      consulo.ui.Window uiWindow = TargetAWT.from((Window)ultimateParentForEventWindow);
      IdeFrame ultimateParentWindowForEvent = IdeFrameUtil.findRootIdeFrame(uiWindow);

      if (!shouldCloseAllPopup && ultimateParentWindowForEvent != null) {
        if (ultimateParentWindowForEvent.isInFullScreen() && !ultimateParentForFocusedComponent.equals(ultimateParentForEventWindow)) {
          shouldCloseAllPopup = true;
        }
      }

      if (shouldCloseAllPopup) {
        closeAllPopups();
      }
    }
    else if (e instanceof KeyEvent) {
      // the following is copied from IdeKeyEventDispatcher
      KeyEvent keyEvent = (KeyEvent)e;
      Object source = keyEvent.getSource();
      if (myIgnoreNextKeyTypedEvent) {
        if (KeyEvent.KEY_TYPED == e.getID()) return true;
        myIgnoreNextKeyTypedEvent = false;
      }
      else if (Platform.current().os().isMac() && InputEvent.ALT_DOWN_MASK == keyEvent.getModifiersEx() && Registry.is("ide.mac.alt.mnemonic.without.ctrl") && source instanceof Component) {
        // the myIgnoreNextKeyTypedEvent changes event processing to support Alt-based mnemonics on Mac only
        if (KeyEvent.KEY_TYPED == e.getID() && !IdeEventQueue.getInstance().isInputMethodEnabled() || IdeKeyEventDispatcher.hasMnemonicInWindow((Component)source, keyEvent)) {
          myIgnoreNextKeyTypedEvent = true;
          return false;
        }
      }
    }

    if (e instanceof KeyEvent || e instanceof MouseEvent) {
      for (int i = myDispatchStack.size() - 1; i >= 0 && i < myDispatchStack.size(); i--) {
        final boolean dispatched = myDispatchStack.get(i).dispatch(e);
        if (dispatched) return true;
      }
    }

    return false;
  }

  public void push(IdePopupEventDispatcher dispatcher) {
    if (!myDispatchStack.contains(dispatcher)) {
      myDispatchStack.add(dispatcher);
    }
  }

  public void remove(IdePopupEventDispatcher dispatcher) {
    myDispatchStack.remove(dispatcher);
  }

  public boolean closeAllPopups(boolean forceRestoreFocus) {
    if (myDispatchStack.isEmpty()) return false;

    boolean closed = true;
    for (IdePopupEventDispatcher each : myDispatchStack) {
      if (forceRestoreFocus) {
        each.setRestoreFocusSilently();
      }
      closed &= each.close();
    }

    return closed;
  }

  public boolean closeAllPopups() {
    return closeAllPopups(true);
  }

  public boolean requestDefaultFocus(boolean forced) {
    if (!isPopupActive()) return false;

    return myDispatchStack.get(myDispatchStack.size() - 1).requestFocus();
  }

  public boolean isPopupWindow(Window w) {
    return myDispatchStack.stream().flatMap(IdePopupEventDispatcher::getPopupStream).filter(popup -> !popup.isDisposed()).map(JBPopup::getContent)
            .anyMatch(jbPopupContent -> SwingUtilities.getWindowAncestor(jbPopupContent) == w);
  }
}
