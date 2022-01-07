// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.popup;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.popup.IdePopupEventDispatcher;
import com.intellij.openapi.ui.popup.JBPopup;
import consulo.disposer.Disposer;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.stream.Stream;

public class PopupDispatcher implements AWTEventListener, KeyEventDispatcher, IdePopupEventDispatcher {

  private static WizardPopup ourActiveWizardRoot;
  private static WizardPopup ourShowingStep;

  private static final PopupDispatcher ourInstance = new PopupDispatcher();

  static {
    // disable this add due it will require get property plugin access
    //if (System.getProperty("is.popup.test") != null || ApplicationManager.getApplication() != null && ApplicationManager.getApplication().isUnitTestMode()) {
    //  Toolkit.getDefaultToolkit().addAWTEventListener(ourInstance, MouseEvent.MOUSE_PRESSED);
    //  KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ourInstance);
    //}
  }

  private PopupDispatcher() {
  }

  public static PopupDispatcher getInstance() {
    return ourInstance;
  }

  static void setActiveRoot(@Nonnull WizardPopup aRootPopup) {
    disposeActiveWizard();
    ourActiveWizardRoot = aRootPopup;
    ourShowingStep = aRootPopup;
    if (ApplicationManager.getApplication() != null) {
      IdeEventQueue.getInstance().getPopupManager().push(ourInstance);
    }
  }

  static void clearRootIfNeeded(@Nonnull WizardPopup aRootPopup) {
    if (ourActiveWizardRoot == aRootPopup) {
      ourActiveWizardRoot = null;
      ourShowingStep = null;
      if (ApplicationManager.getApplication() != null) {
        IdeEventQueue.getInstance().getPopupManager().remove(ourInstance);
      }
    }
  }

  @Override
  public void eventDispatched(AWTEvent event) {
    dispatchMouseEvent(event);
  }

  private static boolean dispatchMouseEvent(@Nonnull AWTEvent event) {
    if (event.getID() != MouseEvent.MOUSE_PRESSED) {
      return false;
    }

    if (ourShowingStep == null) {
      return false;
    }

    WizardPopup eachParent = ourShowingStep;
    final MouseEvent mouseEvent = (MouseEvent)event;

    Point point = (Point)mouseEvent.getPoint().clone();
    SwingUtilities.convertPointToScreen(point, mouseEvent.getComponent());

    while (true) {
      if (eachParent.isDisposed() || !eachParent.getContent().isShowing()) {
        getActiveRoot().cancel();
        return false;
      }

      if (eachParent.getBounds().contains(point) || !eachParent.canClose()) {
        return false;
      }

      eachParent = eachParent.getParent();
      if (eachParent == null) {
        getActiveRoot().cancel();
        return false;
      }
    }
  }

  private static boolean disposeActiveWizard() {
    if (ourActiveWizardRoot != null) {
      ourActiveWizardRoot.disposeChildren();
      Disposer.dispose(ourActiveWizardRoot);
      return true;
    }

    return false;
  }

  @Override
  public boolean dispatchKeyEvent(final KeyEvent e) {
    if (ourShowingStep == null) {
      return false;
    }
    return ourShowingStep.dispatch(e);
  }

  static void setShowing(@Nonnull WizardPopup aBaseWizardPopup) {
    ourShowingStep = aBaseWizardPopup;
  }

  static void unsetShowing(@Nonnull WizardPopup aBaseWizardPopup) {
    if (ourActiveWizardRoot != null) {
      for (WizardPopup wp = aBaseWizardPopup; wp != null; wp = wp.getParent()) {
        if (wp == ourActiveWizardRoot) {
          ourShowingStep = aBaseWizardPopup.getParent();
          return;
        }
      }
    }
  }

  static WizardPopup getActiveRoot() {
    return ourActiveWizardRoot;
  }

  @Override
  public Component getComponent() {
    return ourShowingStep != null && !ourShowingStep.isDisposed() ? ourShowingStep.getContent() : null;
  }

  @Nonnull
  @Override
  public Stream<JBPopup> getPopupStream() {
    return Stream.of(ourActiveWizardRoot);
  }

  @Override
  public boolean dispatch(AWTEvent event) {
    if (event instanceof KeyEvent) {
      return dispatchKeyEvent((KeyEvent)event);
    }
    if (event instanceof MouseEvent) {
      return dispatchMouseEvent(event);
    }
    return false;
  }

  @Override
  public boolean requestFocus() {
    if (ourShowingStep != null) {
      ourShowingStep.requestFocus();
    }

    return true;
  }

  @Override
  public boolean close() {
    return disposeActiveWizard();
  }

  @Override
  public void setRestoreFocusSilently() {
  }
}
