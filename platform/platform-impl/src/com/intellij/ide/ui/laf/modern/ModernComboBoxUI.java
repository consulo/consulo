/*
 * Copyright 2013-2014 must-be.org
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
package com.intellij.ide.ui.laf.modern;

import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.ui.Gray;
import com.intellij.util.ui.UIUtil;
import sun.swing.DefaultLookup;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * @author VISTALL
 * @since 02.08.14
 *
 * Based on {@link com.intellij.ide.ui.laf.darcula.ui.DarculaComboBoxUI}
 */
@SuppressWarnings("GtkPreferredJComboBoxRenderer")
public class ModernComboBoxUI extends BasicComboBoxUI implements Border {
  private final JComboBox myComboBox;
  // Flag for calculating the display size
  private boolean myDisplaySizeDirty = true;

  // Cached the size that the display needs to render the largest item
  private Dimension myDisplaySizeCache = new Dimension(0, 0);
  private Insets myPadding;
  private MouseEnterHandler myMouseEnterHandler;

  public ModernComboBoxUI(JComboBox comboBox) {
    super();
    myComboBox = comboBox;
    myMouseEnterHandler = new MouseEnterHandler(comboBox);
    myComboBox.setBorder(this);
  }

  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  public static ComponentUI createUI(final JComponent c) {
    return new ModernComboBoxUI(((JComboBox)c));
  }

  @Override
  protected void installDefaults() {
    super.installDefaults();
    myPadding = UIManager.getInsets("ComboBox.padding");
  }

  @Override
  protected ListCellRenderer createRenderer() {
    return new BasicComboBoxRenderer.UIResource() {
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        final Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (c instanceof JComponent) {
          final JComponent jc = (JComponent)c;
          if (index == -1) {
            jc.setOpaque(false);
            jc.setForeground(list.getForeground());
          }
          else {
            jc.setOpaque(true);
          }
        }
        return c;
      }
    };
  }

  @Override
  protected JButton createArrowButton() {
    final Color bg = myComboBox.getBackground();
    final Color fg = myComboBox.getForeground();
    JButton button = new BasicArrowButton(SwingConstants.SOUTH, bg, fg, fg, fg) {

      @Override
      public void paint(Graphics g2) {
        final Graphics2D g = (Graphics2D)g2;
        final GraphicsConfig config = new GraphicsConfig(g);

        final int w = getWidth();
        final int h = getHeight();
        g.setColor(UIUtil.getControlColor());
        g.fillRect(0, 0, w, h);
        g.setColor(myComboBox.isEnabled() ? getForeground() : getForeground().darker());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        final int xU = w / 4;
        final int yU = h / 4;
        final Path2D.Double path = new Path2D.Double();
        g.translate(2, 0);
        path.moveTo(xU + 1, yU + 2);
        path.lineTo(3 * xU + 1, yU + 2);
        path.lineTo(2 * xU + 1, 3 * yU);
        path.lineTo(xU + 1, yU + 2);
        path.closePath();
        g.fill(path);
        g.translate(-2, 0);
        g.setColor(getBorderColor());
        g.drawLine(0, -1, 0, h);
        config.restore();
      }

      @Override
      public Dimension getPreferredSize() {
        int size = getFont().getSize() + 4;
        if (size % 2 == 1) size++;
        return new DimensionUIResource(size, size);
      }
    };
    button.setBorder(BorderFactory.createEmptyBorder());
    button.setOpaque(false);
    return button;
  }

  @Override
  protected Insets getInsets() {
    return new InsetsUIResource(4, 7, 4, 5);
  }

  @Override
  protected Dimension getDisplaySize() {
    if (!myDisplaySizeDirty) {
      return new Dimension(myDisplaySizeCache);
    }

    Dimension display = new Dimension();

    ListCellRenderer renderer = comboBox.getRenderer();
    if (renderer == null) {
      renderer = new DefaultListCellRenderer();
    }

    boolean sameBaseline = true;

    Object prototypeValue = comboBox.getPrototypeDisplayValue();
    if (prototypeValue != null) {
      display = getSizeForComponent(renderer.getListCellRendererComponent(listBox, prototypeValue, -1, false, false));
    }
    else {
      final ComboBoxModel model = comboBox.getModel();

      int baseline = -1;
      Dimension d;

      if (model.getSize() > 0) {
        for (int i = 0; i < model.getSize(); i++) {
          Object value = model.getElementAt(i);
          Component rendererComponent = renderer.getListCellRendererComponent(listBox, value, -1, false, false);
          d = getSizeForComponent(rendererComponent);
          if (sameBaseline && value != null && (!(value instanceof String) || !"".equals(value))) {
            int newBaseline = rendererComponent.getBaseline(d.width, d.height);
            if (newBaseline == -1) {
              sameBaseline = false;
            }
            else if (baseline == -1) {
              baseline = newBaseline;
            }
            else if (baseline != newBaseline) {
              sameBaseline = false;
            }
          }
          display.width = Math.max(display.width, d.width);
          display.height = Math.max(display.height, d.height);
        }
      }
      else {
        display = getDefaultSize();
        if (comboBox.isEditable()) {
          display.width = 100;
        }
      }
    }

    if (myPadding != null) {
      display.width += myPadding.left + myPadding.right;
      display.height += myPadding.top + myPadding.bottom;
    }

    myDisplaySizeCache.setSize(display.width, display.height);
    myDisplaySizeDirty = false;

    return display;
  }

  //@Override
  protected Dimension getSizeForComponent(Component comp) {
    currentValuePane.add(comp);
    comp.setFont(comboBox.getFont());
    Dimension d = comp.getPreferredSize();
    currentValuePane.remove(comp);
    return d;
  }


  @Override
  public void paint(Graphics g, JComponent c) {
    final Container parent = c.getParent();
    if (parent != null) {
      g.setColor(parent.getBackground());
      g.fillRect(0, 0, c.getWidth(), c.getHeight());
    }
    paintBorder(c, g, 0, 0, c.getWidth(), c.getHeight());
    hasFocus = comboBox.hasFocus();
    Rectangle r = rectangleForCurrentValue();
    paintCurrentValueBackground(g, r, hasFocus);
    paintCurrentValue(g, r, hasFocus);
  }

  @Override
  public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
    ListCellRenderer renderer = comboBox.getRenderer();
    Component c;

    if (hasFocus && !isPopupVisible(comboBox)) {
      c = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, true, false);
    }
    else {
      c = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false, false);
      c.setBackground(UIManager.getColor("ComboBox.background"));
    }
    c.setFont(comboBox.getFont());
    if (hasFocus && !isPopupVisible(comboBox)) {
      c.setForeground(DefaultLookup.getColor(comboBox, this, "ComboBox.selectionForeground", listBox.getSelectionForeground()));
      c.setBackground(DefaultLookup.getColor(comboBox, this, "ComboBox.selectionBackground", listBox.getSelectionBackground()));
    }
    else {
      if (comboBox.isEnabled()) {
        c.setForeground(comboBox.getForeground());
        c.setBackground(comboBox.getBackground());
      }
      else {
        c.setForeground(DefaultLookup.getColor(comboBox, this, "ComboBox.disabledForeground", null));
        c.setBackground(DefaultLookup.getColor(comboBox, this, "ComboBox.disabledBackground", null));
      }
    }

    // Fix for 4238829: should lay out the JPanel.
    boolean shouldValidate = false;
    if (c instanceof JPanel) {
      shouldValidate = true;
    }

    int x = bounds.x, y = bounds.y, w = bounds.width, h = bounds.height;
    if (myPadding != null) {
      x = bounds.x + myPadding.left;
      y = bounds.y + myPadding.top;
      w = bounds.width - (myPadding.left + myPadding.right);
      h = bounds.height - (myPadding.top + myPadding.bottom);
    }

    currentValuePane.paintComponent(g, c, comboBox, x, y, w, h, shouldValidate);
  }

  @Override
  protected void installKeyboardActions() {
    super.installKeyboardActions();
  }

  @Override
  public void paintBorder(Component c, Graphics g2, int x, int y, int width, int height) {
    if (comboBox == null || arrowButton == null) {
      return; //NPE on LaF change
    }

    hasFocus = c.isEnabled() && myMouseEnterHandler.isMouseEntered();

    final Graphics2D g = (Graphics2D)g2;
    final Rectangle arrowButtonBounds = arrowButton.getBounds();
    final int xxx = arrowButtonBounds.x - 5;
    final GraphicsConfig config = new GraphicsConfig(g);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
    if (editor != null && comboBox.isEditable()) {
      ((JComponent)editor).setBorder(null);
      g.setColor(editor.getBackground());
      g.fillRect(x + 1, y + 1, width - 2, height - 4);
      g.setColor(arrowButton.getBackground());
      g.fillRect(xxx, y + 1, width - xxx, height - 4);
      g.setColor(editor.getBackground());
      g.fillRect(xxx, y + 1, 5, height - 4);
    }
    else {
      g.setColor(UIUtil.getPanelBackground());
      g.fillRect(x + 1, y + 1, width - 2, height - 4);
    }

    final Color borderColor = getBorderColor();//ColorUtil.shift(UIUtil.getBorderColor(), 4);
    g.setColor(borderColor);
    int off = hasFocus ? 1 : 0;
    g.drawLine(xxx + 5, y + 1 + off, xxx + 5, height - 3);

    Rectangle r = rectangleForCurrentValue();
    paintCurrentValueBackground(g, r, hasFocus);
    paintCurrentValue(g, r, hasFocus);

    if (hasFocus) {
      g.setColor(ModernUIUtil.getSelectionBackground());
      g.drawRect(1, 1, width - 2, height - 4);
    }
    else {
      g.setColor(borderColor);
      g.drawRect(1, 1, width - 2, height - 4);
    }
    config.restore();
  }

  private static Gray getBorderColor() {
    return Gray._100;
  }

  @Override
  public Insets getBorderInsets(Component c) {
    return new InsetsUIResource(4, 7, 4, 5);
  }

  @Override
  public boolean isBorderOpaque() {
    return false;
  }
}