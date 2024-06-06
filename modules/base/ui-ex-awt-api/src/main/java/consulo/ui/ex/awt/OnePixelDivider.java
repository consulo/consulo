/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.component.util.Weighted;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.platform.Platform;
import consulo.ui.ex.IdeGlassPane;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.Splittable;
import consulo.ui.ex.awt.util.IdeGlassPaneUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;

/**
 * @author Konstantin Bulenkov
 */
public class OnePixelDivider extends Divider {
  public static final Color BACKGROUND = new JBColor(() -> {
    final Color bg = UIManager.getColor("OnePixelDivider.background");
    return bg != null ? bg : JBColor.border();
  });

  private boolean myVertical;
  private Splittable mySplitter;
  private boolean myResizeEnabled;
  private boolean mySwitchOrientationEnabled;
  protected Point myPoint;
  private IdeGlassPane myGlassPane;
  private final MouseAdapter myListener = new MyMouseAdapter();
  private Disposable myDisposable;

  public OnePixelDivider(boolean vertical, Splittable splitter) {
    super(new GridBagLayout());
    mySplitter = splitter;
    myResizeEnabled = true;
    mySwitchOrientationEnabled = false;
    setFocusable(false);
    enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    setOrientation(vertical);
    setBackground(BACKGROUND);
  }

  @Override
  public void paint(Graphics g) {
    final Rectangle bounds = g.getClipBounds();
    if (mySplitter instanceof OnePixelSplitter) {
      final Supplier<Insets> blindZone = ((OnePixelSplitter)mySplitter).getBlindZone();
      if (blindZone != null) {
        final Insets insets = blindZone.get();
        if (insets != null) {
          bounds.x += insets.left;
          bounds.y += insets.top;
          bounds.width -= insets.left + insets.right;
          bounds.height -= insets.top + insets.bottom;
          g.setColor(getBackground());
          g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
          return;
        }
      }
    }
    super.paint(g);
  }

  @Override
  public void addNotify() {
    super.addNotify();
    init();
  }

  @Override
  public void removeNotify() {
    super.removeNotify();
    if (myDisposable != null && !Disposer.isDisposed(myDisposable)) {
      Disposer.dispose(myDisposable);
    }
  }

  private boolean myDragging = false;

  private void setDragging(boolean dragging) {
    if (myDragging != dragging) {
      myDragging = dragging;
      mySplitter.setDragging(dragging);
    }
  }
  private class MyMouseAdapter extends MouseAdapter implements Weighted {
    @Override
    public void mousePressed(MouseEvent e) {
      setDragging(isInDragZone(e));
      _processMouseEvent(e);
      if (myDragging) {
        e.consume();
      }
    }

    boolean isInDragZone(MouseEvent e) {
      MouseEvent event = getTargetEvent(e);
      Point p = event.getPoint();
      boolean vertical = isVertical();
      OnePixelDivider d = OnePixelDivider.this;
      if ((vertical ? p.x : p.y) < 0 || vertical && p.x > d.getWidth() || !vertical && p.y > d.getHeight()) return false;
      int r = Math.abs(vertical ? p.y : p.x);
      return r < JBUI.scale(6);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      _processMouseEvent(e);
      setDragging(false);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      final OnePixelDivider divider = OnePixelDivider.this;
      if (isInDragZone(e)) {
        myGlassPane.setCursor(divider.getCursor(), divider);
      } else {
        myGlassPane.setCursor(null, divider);
      }
      _processMouseMotionEvent(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
      _processMouseMotionEvent(e);
    }
    @Override
    public double getWeight() {
      return 1;
    }
    private void _processMouseMotionEvent(MouseEvent e) {
      MouseEvent event = getTargetEvent(e);
      if (event == null) {
        myGlassPane.setCursor(null, myListener);
        return;
      }

      processMouseMotionEvent(event);
      if (event.isConsumed()) {
        e.consume();
      }
    }

    private void _processMouseEvent(MouseEvent e) {
      MouseEvent event = getTargetEvent(e);
      if (event == null) {
        myGlassPane.setCursor(null, myListener);
        return;
      }

      processMouseEvent(event);
      if (event.isConsumed()) {
        e.consume();
      }
    }
  }

  private MouseEvent getTargetEvent(MouseEvent e) {
    return SwingUtilities.convertMouseEvent(e.getComponent(), e, this);
  }

  private void init() {
    myGlassPane = IdeGlassPaneUtil.find(this);
    myDisposable = Disposable.newDisposable();
    myGlassPane.addMouseMotionPreprocessor(myListener, myDisposable);
    myGlassPane.addMousePreprocessor(myListener, myDisposable);
  }

  @Override
  public void setOrientation(boolean vertical) {
    removeAll();
    myVertical = vertical;
    final int cursorType = isVertical() ? Cursor.N_RESIZE_CURSOR : Cursor.W_RESIZE_CURSOR;
    setCursor(Cursor.getPredefinedCursor(cursorType));
  }

  @Override
  protected void processMouseMotionEvent(MouseEvent e) {
    super.processMouseMotionEvent(e);
    if (!myResizeEnabled) return;
    if (MouseEvent.MOUSE_DRAGGED == e.getID() && myDragging) {
      myPoint = SwingUtilities.convertPoint(this, e.getPoint(), mySplitter.asComponent());
      float proportion;
      final float firstMinProportion = mySplitter.getMinProportion(true);
      final float secondMinProportion = mySplitter.getMinProportion(false);
      if (isVertical()) {
        if (getHeight() > 0) {
          proportion = Math.min(1.0f, Math
                  .max(.0f, Math.min(Math.max(firstMinProportion, (float)myPoint.y / (float)mySplitter.asComponent().getHeight()), 1 - secondMinProportion)));
          mySplitter.setProportion(proportion);
        }
      }
      else {
        if (getWidth() > 0) {
          proportion = Math.min(1.0f, Math.max(.0f, Math.min(
                  Math.max(firstMinProportion, (float)myPoint.x / (float)mySplitter.asComponent().getWidth()), 1 - secondMinProportion)));
          mySplitter.setProportion(proportion);
        }
      }
      e.consume();
    }
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    super.processMouseEvent(e);
    if (e.getID() == MouseEvent.MOUSE_CLICKED) {
      if (mySwitchOrientationEnabled
          && e.getClickCount() == 1
          && SwingUtilities.isLeftMouseButton(e) && (Platform.current().os().isMac() ? e.isMetaDown() : e.isControlDown())) {
        mySplitter.setOrientation(!mySplitter.getOrientation());
      }
      if (myResizeEnabled && e.getClickCount() == 2) {
        mySplitter.setProportion(.5f);
      }
    }
  }

  @Override
  public void setResizeEnabled(boolean resizeEnabled) {
    myResizeEnabled = resizeEnabled;
    if (!myResizeEnabled) {
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    else {
      setCursor(isVertical() ?
                Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR) :
                Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
    }
  }

  @Override
  public void setSwitchOrientationEnabled(boolean switchOrientationEnabled) {
    mySwitchOrientationEnabled = switchOrientationEnabled;
  }


  public boolean isVertical() {
    return myVertical;
  }
}