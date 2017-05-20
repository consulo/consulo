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
package com.intellij.ui;

import com.intellij.util.ui.JBInsets;

import javax.swing.*;
import java.awt.*;

/**
 * Cell renderer CPU optimization.
 * @see javax.swing.table.DefaultTableCellRenderer#invalidate()
 *
 * @author gregsh
 */
public class CellRendererPanel extends JPanel {

  private boolean mySelected;

  public CellRendererPanel() {
    super(null); // we do the layout ourselves
  }

  public final boolean isSelected() {
    return mySelected;
  }

  public final void setSelected(boolean isSelected) {
    mySelected = isSelected;
  }

  // property change support ----------------
  @Override
  protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
  }

  @Override
  public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
  }

  // isOpaque() optimization ----------------
  @Override
  public boolean isOpaque() {
    return false;
  }

  @Override
  protected void paintComponent(Graphics g) {
    if (mySelected) {
      g.setColor(getBackground());
      g.fillRect(0, 0, getWidth(), getHeight());
    }
  }

  // BEGIN no validation methods --------------
  @Override
  public void doLayout() {
    int count = getComponentCount();
    if (count == 1) {
      Rectangle bounds = new Rectangle(getWidth(), getHeight());
      JBInsets.removeFrom(bounds, getInsets());
      Component child = getComponent(0);
      child.setBounds(bounds);
      if (child instanceof CellRendererPanel) {
        ((CellRendererPanel)child).invalidateLayout();
        child.doLayout();
      }
    }
    else {
      invalidateLayout();
      super.doLayout();
      for (int i = 0; i < count; i++) {
        Component c = getComponent(i);
        if (c instanceof CellRendererPanel) {
          c.doLayout();
        }
      }
    }
  }

  @Override
  public Dimension getPreferredSize() {
    if (getComponentCount() != 1) {
      return super.getPreferredSize();
    }
    return getComponent(0).getPreferredSize();
  }

  @Override
  public void invalidate() {
  }

  private void invalidateLayout() {
    LayoutManager layout = getLayout();
    if (layout instanceof LayoutManager2) {
      ((LayoutManager2)layout).invalidateLayout(this);
    }
  }

  @Override
  public void validate() {
    doLayout();
  }

  @Override
  public void revalidate() {
  }

  @Override
  public void repaint(long tm, int x, int y, int width, int height) {
  }

  @Override
  public void repaint(Rectangle r) {
  }

  @Override
  public void repaint() {
  }

// END no validation methods --------------
}
