/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package consulo.desktop.awt.ui.popup;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.ide.impl.idea.ui.popup.AbstractPopup;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.StackingPopupDispatcher;
import consulo.util.collection.WeakList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Stack;
import java.util.stream.Stream;

@Singleton
@ServiceImpl
public class StackingPopupDispatcherImpl extends StackingPopupDispatcher implements AWTEventListener, KeyEventDispatcher {

  private final Stack<JBPopup> myStack = new Stack<>();
  private final Collection<JBPopup> myPersistentPopups = new WeakList<>();

  private final Collection<JBPopup> myAllPopups = new WeakList<>();


  @Inject
  public StackingPopupDispatcherImpl() {
  }

  @Override
  public void onPopupShown(JBPopup popup, boolean inStack) {
    if (inStack) {
      myStack.push(popup);
      if (ApplicationManager.getApplication() != null) {
        IdeEventQueue.getInstance().getPopupManager().push(getInstance());
      }
    }
    else if (popup.isPersistent()) {
      myPersistentPopups.add(popup);
    }

    myAllPopups.add(popup);
  }

  @Override
  public void onPopupHidden(JBPopup popup) {
    boolean wasInStack = myStack.remove(popup);
    myPersistentPopups.remove(popup);

    if (wasInStack && myStack.isEmpty()) {
      if (ApplicationManager.getApplication() != null) {
        IdeEventQueue.getInstance().getPopupManager().remove(this);
      }
    }

    myAllPopups.remove(popup);
  }

  @Override
  public void hidePersistentPopups() {
    for (JBPopup each : myPersistentPopups) {
      if (each.isNativePopup()) {
        each.setUiVisible(false);
      }
    }
  }

  @Override
  public void restorePersistentPopups() {
    for (JBPopup each : myPersistentPopups) {
      if (each.isNativePopup()) {
        each.setUiVisible(true);
      }
    }
  }

  @Override
  public void eventDispatched(AWTEvent event) {
    dispatchMouseEvent(event);
  }

  private boolean dispatchMouseEvent(AWTEvent event) {
    if (event.getID() != MouseEvent.MOUSE_PRESSED) {
      return false;
    }

    if (myStack.isEmpty()) {
      return false;
    }

    AbstractPopup popup = (AbstractPopup)findPopup();

    final MouseEvent mouseEvent = (MouseEvent)event;

    Point point = (Point)mouseEvent.getPoint().clone();
    SwingUtilities.convertPointToScreen(point, mouseEvent.getComponent());

    while (true) {
      if (popup != null && !popup.isDisposed()) {
        Window window = UIUtil.getWindow(mouseEvent.getComponent());
        if (window != null && window != popup.getPopupWindow() && SwingUtilities.isDescendingFrom(window, popup.getPopupWindow())) {
          return false;
        }
        final Component content = popup.getContent();
        if (!content.isShowing()) {
          popup.cancel();
          return false;
        }

        final Rectangle bounds = new Rectangle(content.getLocationOnScreen(), content.getSize());
        if (bounds.contains(point) || !popup.isCancelOnClickOutside()) {
          return false;
        }

        if (!popup.canClose()) {
          return false;
        }

        //click on context menu item
        if (MenuSelectionManager.defaultManager().getSelectedPath().length > 0) {
          return false;
        }

        popup.cancel(mouseEvent);
      }

      if (myStack.isEmpty()) {
        return false;
      }

      popup = (AbstractPopup)myStack.peek();
      if (popup == null || popup.isDisposed()) {
        myStack.pop();
      }
    }
  }

  @Nullable
  private JBPopup findPopup() {
    while (true) {
      if (myStack.isEmpty()) break;
      final AbstractPopup each = (AbstractPopup)myStack.peek();
      if (each == null || each.isDisposed()) {
        myStack.pop();
      }
      else {
        return each;
      }
    }

    return null;
  }

  @Override
  public boolean dispatchKeyEvent(final KeyEvent e) {
    final boolean closeRequest = AbstractPopup.isCloseRequest(e);

    JBPopup popup = closeRequest ? findPopup() : getFocusedPopup();
    if (popup == null) return false;

    Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
    if (window instanceof Dialog && ((Dialog)window).isModal()) {
      if (!SwingUtilities.isDescendingFrom(popup.getContent(), window)) return false;
    }

    return popup.dispatchKeyEvent(e);
  }

  @Override
  @Nullable
  public Component getComponent() {
    return myStack.isEmpty() || myStack.peek().isDisposed() ? null : myStack.peek().getContent();
  }

  @Nonnull
  @Override
  public Stream<JBPopup> getPopupStream() {
    return myStack.stream();
  }

  @Override
  public boolean dispatch(AWTEvent event) {
    if (event instanceof KeyEvent) {
      return dispatchKeyEvent((KeyEvent)event);
    }
    return event instanceof MouseEvent && dispatchMouseEvent(event);
  }

  @Override
  public boolean requestFocus() {
    if (myStack.isEmpty()) return false;

    final AbstractPopup popup = (AbstractPopup)myStack.peek();
    return popup.requestFocus();
  }

  @Override
  public boolean close() {
    if (!closeActivePopup()) return false;

    int size = myStack.size();
    while (closeActivePopup()) {
      int next = myStack.size();
      if (size == next) {
        // no popup was actually closed, break
        break;
      }
      size = next;
    }
    return true; // at least one popup was closed
  }

  @Override
  public void setRestoreFocusSilently() {
    if (myStack.isEmpty()) return;

    for (JBPopup each : myAllPopups) {
      if (each instanceof AbstractPopup) {
        ((AbstractPopup)each).setOk(true);
      }
    }

  }

  @Override
  public boolean closeActivePopup() {
    if (myStack.isEmpty()) return false;

    final AbstractPopup popup = (AbstractPopup)myStack.peek();
    if (popup != null && popup.isVisible() && popup.isCancelOnWindowDeactivation() && popup.canClose()) {
      popup.cancel();
      // setCancelCallback(..) can override cancel()
      return !popup.isVisible();
    }
    return false;
  }

  @Override
  public boolean isPopupFocused() {
    return getFocusedPopup() != null;
  }

  private JBPopup getFocusedPopup() {
    for (JBPopup each : myAllPopups) {
      if (each != null && each.isFocused()) return each;
    }
    return null;
  }
}
