/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import consulo.util.lang.ObjectUtil;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * @author nik
 */
public class TextComponentEmptyText extends StatusText {
  private final JTextComponent myOwner;
  private String myStatusTriggerText = "";

  public TextComponentEmptyText(JTextComponent owner) {
    super(owner);
    myOwner = owner;
    clear();
    myOwner.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        myOwner.repaint();
      }

      @Override
      public void focusLost(FocusEvent e) {
        myOwner.repaint();
      }
    });
  }

  public void setTextToTriggerStatus(@Nonnull String defaultText) {
    myStatusTriggerText = defaultText;
  }

  @Nonnull
  public String getStatusTriggerText() {
    return myStatusTriggerText;
  }

  public void paintStatusText(Graphics g) {
    if (!isFontSet()) {
      getComponent().setFont(myOwner.getFont());
      getSecondaryComponent().setFont(myOwner.getFont());
    }
    paint(myOwner, g);
  }

  @Override
  protected boolean isStatusVisible() {
    return myOwner.getText().equals(myStatusTriggerText) && !myOwner.isFocusOwner();
  }

  @Override
  protected Rectangle getTextComponentBound() {
    Rectangle b = myOwner.getBounds();
    Insets insets = ObjectUtil.notNull(myOwner.getInsets(), JBUI.emptyInsets());
    Insets margin = ObjectUtil.notNull(myOwner.getMargin(), JBUI.emptyInsets());
    Insets ipad = getComponent().getIpad();
    int left = insets.left + margin.left - ipad.left;
    int right = insets.right + margin.right - ipad.right;
    int top = insets.top + margin.top - ipad.top;
    int bottom = insets.bottom + margin.bottom - ipad.bottom;
    return new Rectangle(left, top, b.width - left - right, b.height - top - bottom);
  }

  @Nonnull
  @Override
  protected Rectangle adjustComponentBounds(@Nonnull JComponent component, @Nonnull Rectangle bounds) {
    if (isVerticalFlow()) {
      return super.adjustComponentBounds(component, bounds);
    }
    else {
      Dimension size = component.getPreferredSize();
      return component == myComponent ? new Rectangle(bounds.x, bounds.y, size.width, bounds.height) : new Rectangle(bounds.x + bounds.width - size.width, bounds.y, size.width, bounds.height);
    }
  }
}
