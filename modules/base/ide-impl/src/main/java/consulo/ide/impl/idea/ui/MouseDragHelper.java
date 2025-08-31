/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.application.ui.UISettings;
import consulo.ui.ex.awt.NullableComponent;
import consulo.component.util.Weighted;
import consulo.ui.ex.IdeGlassPane;
import consulo.ui.ex.awt.util.IdeGlassPaneUtil;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.update.Activatable;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public abstract class MouseDragHelper implements MouseListener, MouseMotionListener, KeyEventDispatcher, Weighted {

  public static final int DRAG_START_DEADZONE = 7;

  private final JComponent myDragComponent;

  private Point myPressPointScreen;
  protected Point myPressedOnScreenPoint;
  private Point myPressPointComponent;

  private boolean myDraggingNow;
  private boolean myDragJustStarted;
  private IdeGlassPane myGlassPane;
  private final Disposable myParentDisposable;
  private Dimension myDelta;

  private boolean myDetachPostponed;
  private boolean myDetachingMode;
  private boolean myCancelled;

  public MouseDragHelper(Disposable parent, JComponent dragComponent) {
    myDragComponent = dragComponent;
    myParentDisposable = parent;

  }

  /**
   * @param event
   * @return false if Settings -> Appearance -> Drag-n-Drop with ALT pressed only is selected but event doesn't have ALT modifier
   */
  public static boolean checkModifiers(InputEvent event) {
    if (event == null || !UISettings.getInstance().getDndWithPressedAltOnly()) return true;
    return (event.getModifiers() & InputEvent.ALT_MASK) != 0;
  }

  public void start() {
    if (myGlassPane != null) return;

    new UiNotifyConnector(myDragComponent, new Activatable() {
      @Override
      public void showNotify() {
        attach();
      }

      @Override
      public void hideNotify() {
        detach(true);
      }
    });

    Disposer.register(myParentDisposable, this::stop);
  }

  private void attach() {
    if (myDetachPostponed) {
      myDetachPostponed = false;
      return;
    }
    myGlassPane = IdeGlassPaneUtil.find(myDragComponent);
    myGlassPane.addMousePreprocessor(this, myParentDisposable);
    myGlassPane.addMouseMotionPreprocessor(this, myParentDisposable);
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    Disposer.register(myParentDisposable, () -> KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(MouseDragHelper.this));
  }

  public void stop() {
    detach(false);
  }

  private void detach(boolean canPostponeDetach) {
    if (canPostponeDetach && myDraggingNow) {
      myDetachPostponed = true;
      return;
    }
    if (myGlassPane != null) {
      myGlassPane.removeMousePreprocessor(this);
      myGlassPane.removeMouseMotionPreprocessor(this);
      KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
      myGlassPane = null;
    }
  }

  @Override
  public double getWeight() {
    return 2;
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if (!canStartDragging(e)) return;

    myPressPointScreen = new RelativePoint(e).getScreenPoint();
    myPressedOnScreenPoint = new Point(myPressPointScreen);
    myPressPointComponent = e.getPoint();
    processMousePressed(e);

    myDelta = new Dimension();
    if (myDragComponent.isShowing()) {
      Point delta = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), myDragComponent);
      myDelta.width = delta.x;
      myDelta.height = delta.y;
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (myCancelled) {
      myCancelled = false;
      return;
    }
    boolean wasDragging = myDraggingNow;
    myPressPointScreen = null;
    myDraggingNow = false;
    myDragJustStarted = false;

    if (wasDragging) {
      try {
        if (myDetachingMode) {
          processDragOutFinish(e);
        }
        else {
          processDragFinish(e, false);
        }
      }
      finally {
        myPressedOnScreenPoint = null;
        resetDragState();
        e.consume();
        if (myDetachPostponed) {
          myDetachPostponed = false;
          detach(false);
        }
      }
    }
  }

  private void resetDragState() {
    myDraggingNow = false;
    myDragJustStarted = false;
    myPressPointComponent = null;
    myPressPointScreen = null;
    myDetachingMode = false;
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (myPressPointScreen == null || myCancelled) return;

    boolean deadZone = isWithinDeadZone(e);
    if (!myDraggingNow && !deadZone) {
      myDraggingNow = true;
      myDragJustStarted = true;
    }
    else if (myDraggingNow) {
      myDragJustStarted = false;
    }

    if (myDraggingNow && myPressPointScreen != null) {
      Point draggedTo = new RelativePoint(e).getScreenPoint();

      boolean dragOutStarted = false;
      if (!myDetachingMode) {
        if (isDragOut(e, draggedTo, (Point)myPressPointScreen.clone())) {
          myDetachingMode = true;
          processDragFinish(e, true);
          dragOutStarted = true;
        }
      }

      if (myDetachingMode) {
        processDragOut(e, draggedTo, (Point)myPressPointScreen.clone(), dragOutStarted);
      }
      else {
        processDrag(e, draggedTo, (Point)myPressPointScreen.clone());
      }
    }
  }

  private boolean canStartDragging(MouseEvent me) {
    if (me.getButton() != MouseEvent.BUTTON1) return false;
    if (!myDragComponent.isShowing()) return false;

    Component component = me.getComponent();
    if (NullableComponent.Check.isNullOrHidden(component)) return false;
    Point dragComponentPoint = SwingUtilities.convertPoint(me.getComponent(), me.getPoint(), myDragComponent);
    return canStartDragging(myDragComponent, dragComponentPoint);
  }

  protected boolean canStartDragging(JComponent dragComponent, Point dragComponentPoint) {
    return true;
  }

  protected void processMousePressed(MouseEvent event) {
  }

  protected void processDragCancel() {
  }

  protected void processDragFinish(MouseEvent event, boolean willDragOutStart) {
  }

  protected void processDragOutFinish(MouseEvent event) {
  }

  protected void processDragOutCancel() {
  }

  public final boolean isDragJustStarted() {
    return myDragJustStarted;
  }

  protected abstract void processDrag(MouseEvent event, Point dragToScreenPoint, Point startScreenPoint);

  protected boolean isDragOut(MouseEvent event, Point dragToScreenPoint, Point startScreenPoint) {
    return false;
  }

  protected void processDragOut(MouseEvent event, Point dragToScreenPoint, Point startScreenPoint, boolean justStarted) {
    event.consume();
  }

  private boolean isWithinDeadZone(MouseEvent e) {
    Point screen = new RelativePoint(e).getScreenPoint();
    return Math.abs(myPressPointScreen.x - screen.x - myDelta.width) < DRAG_START_DEADZONE && Math.abs(myPressPointScreen.y - screen.y - myDelta.height) < DRAG_START_DEADZONE;
  }

  @Override
  public void mouseClicked(MouseEvent e) {
  }

  @Override
  public void mouseEntered(MouseEvent e) {
  }

  @Override
  public void mouseExited(MouseEvent e) {
  }

  @Override
  public void mouseMoved(MouseEvent e) {
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_ESCAPE && e.getID() == KeyEvent.KEY_PRESSED && myDraggingNow) {
      myCancelled = true;
      if (myDetachingMode) {
        processDragOutCancel();
      }
      else {
        processDragCancel();
      }
      resetDragState();
      return true;
    }
    return false;
  }
}
