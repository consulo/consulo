/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.wm.impl;

import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.WindowInfo;
import consulo.desktop.swt.ui.impl.SWTComponentDelegate;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ToolWindowStripeButton;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.internal.win32.RECT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 12/12/2021
 */
public class DesktopSwtToolWindowStripeButtonImpl extends SWTComponentDelegate<DesktopSwtToolWindowStripeButtonImpl.StripeButton> implements ToolWindowStripeButton {
  public class StripeButton extends Canvas {

    private String myText = "";

    private boolean myHovered;

    public StripeButton(Composite parent, int style) {
      super(parent, style);

      myText = myInternalDecorator.getWindowInfo().getId();

      addMouseTrackListener(new MouseTrackAdapter() {
        @Override
        public void mouseEnter(MouseEvent e) {
          myHovered = true;
          redraw();
        }

        @Override
        public void mouseExit(MouseEvent e) {
          myHovered = false;
          redraw();
        }
      });

      addPaintListener(paintEvent -> {
        GC gc = paintEvent.gc;

        Point size = getSize();
        if (isVertical()) {
          Transform tr = new Transform(paintEvent.display);
          tr.rotate(-90);
          gc.setTransform(tr);

          gc.setBackground(new Color(0, 255, 0));
          gc.drawRectangle(0, 0, size.x, size.y);

          gc.drawText(myText, 0, 0);

          tr.dispose();
        }
        else {
          if (myHovered) {
            gc.setBackground(new Color(55, 55, 55, 85));
            gc.drawRectangle(0, 0, size.x, size.y);
          }

          gc.drawText(myText, 0, 0);
        }
      });
    }


    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
      Composite parent = getParent();

      Point parentSize = parent.getSize();

      int textLength = myText.length() * 2;

      long hDC = OS.GetDC(handle);
      try {
        char[] buffer = myText.toCharArray();

        int flags = OS.DT_CALCRECT | OS.DT_EDITCONTROL | OS.DT_NOPREFIX;

        RECT rect = new RECT();

        OS.DrawText(hDC, buffer, buffer.length, rect, flags);

        textLength = Math.max(textLength, rect.right - rect.left);
      }
      finally {
        OS.ReleaseDC(handle, hDC);
      }

      boolean isVertical = isVertical();

      if (isVertical) {
        return new Point(parentSize.x, textLength);
      }
      else {
        return new Point(textLength, parentSize.y);
      }
    }
  }

  private final DesktopSwtToolWindowInternalDecorator myInternalDecorator;

  public DesktopSwtToolWindowStripeButtonImpl(DesktopSwtToolWindowInternalDecorator internalDecorator, DesktopSwtToolWindowPanelImpl toolWindowPanel) {
    myInternalDecorator = internalDecorator;
  }

  @Override
  protected StripeButton createSWT(Composite parent) {
    return new StripeButton(parent, SWT.NONE);
  }

  private boolean isVertical() {
    ToolWindowAnchor anchor = myInternalDecorator.getWindowInfo().getAnchor();
    return anchor == ToolWindowAnchor.LEFT || anchor == ToolWindowAnchor.RIGHT;
  }

  @Override
  protected void initialize(StripeButton component) {
    super.initialize(component);
  }

  @Nonnull
  @Override
  public WindowInfo getWindowInfo() {
    return myInternalDecorator.getWindowInfo();
  }

  @Override
  public void apply(@Nonnull WindowInfo windowInfo) {

  }

  @RequiredUIAccess
  @Override
  public void updatePresentation() {

  }

  @Nonnull
  @Override
  public Component getComponent() {
    return this;
  }
}
