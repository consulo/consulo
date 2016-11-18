/*
 * Copyright 2000-2009 JetBrains s.r.o.
 * Copyright 2013-2016 consulo.io
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
package com.intellij.openapi.actionSystem.ex;

import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.MouseEventAdapter;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * @author VISTALL
 * @since 25-Oct-16
 * <p>
 * This is extract code from {@link ComboBoxButton}
 */
public class ComboBoxButtonUI extends BasicButtonUI {
  protected boolean myMouseInside;

  public static ComponentUI createUI(JComponent c) {
    return new ComboBoxButtonUI((ComboBoxButton)c);
  }

  public ComboBoxButtonUI(ComboBoxButton comboBoxButton) {
    //noinspection HardCodedStringLiteral
    comboBoxButton.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        myMouseInside = true;
        comboBoxButton.repaint();
      }

      @Override
      public void mouseExited(MouseEvent e) {
        myMouseInside = false;
        comboBoxButton.repaint();
      }

      @Override
      public void mousePressed(final MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
          e.consume();
          comboBoxButton.doClick();
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        dispatchEventToPopup(comboBoxButton, e);
      }
    });

    comboBoxButton.addMouseMotionListener(new MouseMotionListener() {
      @Override
      public void mouseDragged(MouseEvent e) {
        mouseMoved(
                MouseEventAdapter.convert(e, e.getComponent(), MouseEvent.MOUSE_MOVED, e.getWhen(), e.getModifiers() | e.getModifiersEx(), e.getX(), e.getY()));
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        dispatchEventToPopup(comboBoxButton, e);
      }
    });

    setupBorder(comboBoxButton);
  }

  protected void setupBorder(ComboBoxButton comboBoxButton) {
    comboBoxButton.setBorder(new AbstractBorder() {
      @Override
      public Insets getBorderInsets(Component c) {
        final Insets insets = super.getBorderInsets(c);
        //noinspection UseDPIAwareInsets
        return new Insets(insets.top, insets.left + JBUI.scale(2), insets.bottom, insets.right + JBUI.scale(2) + getArrowIcon().getIconWidth());
      }
    });
  }

  public static Icon getArrowIcon() {
    return UIUtil.isUnderDarkBuildInLaf() ? AllIcons.General.ComboArrow : AllIcons.General.ComboBoxButtonArrow;
  }

  public static Icon getDisabledArrowIcon() {
    return IconLoader.getDisabledIcon(getArrowIcon());
  }

  // Event forwarding. We need it if user does press-and-drag gesture for opening popup and choosing item there.
  // It works in JComboBox, here we provide the same behavior
  private void dispatchEventToPopup(ComboBoxButton comboBoxButton, MouseEvent e) {
    JBPopup popup = comboBoxButton.getPopup();

    if (popup != null && popup.isVisible()) {
      JComponent content = popup.getContent();
      Rectangle rectangle = content.getBounds();
      Point location = rectangle.getLocation();
      SwingUtilities.convertPointToScreen(location, content);
      Point eventPoint = e.getLocationOnScreen();
      rectangle.setLocation(location);
      if (rectangle.contains(eventPoint)) {
        MouseEvent event = SwingUtilities.convertMouseEvent(e.getComponent(), e, popup.getContent());
        Component component = SwingUtilities.getDeepestComponentAt(content, event.getX(), event.getY());
        if (component != null) component.dispatchEvent(event);
      }
    }
  }

  @Override
  public Dimension getMinimumSize(JComponent c) {
    return new Dimension(super.getMinimumSize(c).width, getPreferredSize(c).height);
  }

  @Override
  public Dimension getPreferredSize(JComponent c) {
    ComboBoxButton comboBoxButton = (ComboBoxButton)c;
    ComboBoxAction comboBoxAction = comboBoxButton.getComboBoxAction();

    final boolean isEmpty = comboBoxButton.getIcon() == null && StringUtil.isEmpty(comboBoxButton.getText());
    int width = isEmpty ? JBUI.scale(10) + getArrowIcon().getIconWidth() : super.getPreferredSize(c).width;
    if (comboBoxAction.isSmallVariant()) width += JBUI.scale(4);
    return new Dimension(width, comboBoxAction.isSmallVariant() ? JBUI.scale(19) : super.getPreferredSize(c).height);
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    ComboBoxButton comboBoxButton = (ComboBoxButton)c;
    ComboBoxAction comboBoxAction = comboBoxButton.getComboBoxAction();

    UISettings.setupAntialiasing(g);
    final Dimension size = comboBoxButton.getSize();
    final boolean isEmpty = comboBoxButton.getIcon() == null && StringUtil.isEmpty(comboBoxButton.getText());

    final Color textColor = comboBoxButton.isEnabled() ? UIManager.getColor("Panel.foreground") : UIUtil.getInactiveTextColor();
    if (comboBoxButton.isForceTransparent()) {
      final Icon icon = comboBoxButton.getIcon();
      int x = 7;
      if (icon != null) {
        icon.paintIcon(c, g, x, (size.height - icon.getIconHeight()) / 2);
        x += icon.getIconWidth() + 3;
      }
      if (!StringUtil.isEmpty(comboBoxButton.getText())) {
        final Font font = comboBoxButton.getFont();
        g.setFont(font);
        g.setColor(textColor);
        g.drawString(comboBoxButton.getText(), x, (size.height + font.getSize()) / 2 - 1);
      }
    }
    else {

      if (comboBoxAction.isSmallVariant()) {
        final Graphics2D g2 = (Graphics2D)g;
        g2.setColor(UIUtil.getControlColor());
        final int w = comboBoxButton.getWidth();
        final int h = comboBoxButton.getHeight();
        if (comboBoxButton.getModel().isArmed() && comboBoxButton.getModel().isPressed()) {
          g2.setPaint(UIUtil.getGradientPaint(0, 0, UIUtil.getControlColor(), 0, h, ColorUtil.shift(UIUtil.getControlColor(), 0.8)));
        }
        else {
          if (UIUtil.isUnderDarcula()) {
            g2.setPaint(UIUtil.getGradientPaint(0, 0, ColorUtil.shift(UIUtil.getControlColor(), 1.1), 0, h, ColorUtil.shift(UIUtil.getControlColor(), 0.9)));
          }
          else {
            g2.setPaint(UIUtil.getGradientPaint(0, 0, new JBColor(SystemInfo.isMac ? Gray._226 : Gray._245, Gray._131), 0, h,
                                                new JBColor(SystemInfo.isMac ? Gray._198 : Gray._208, Gray._128)));
          }
        }
        g2.fillRoundRect(2, 0, w - 2, h, 5, 5);

        Color borderColor = myMouseInside ? new JBColor(Gray._111, Gray._118) : new JBColor(Gray._151, Gray._95);
        g2.setPaint(borderColor);
        g2.drawRoundRect(2, 0, w - 3, h - 1, 5, 5);

        final Icon icon = comboBoxButton.getIcon();
        int x = 7;
        if (icon != null) {
          icon.paintIcon(c, g, x, (size.height - icon.getIconHeight()) / 2);
          x += icon.getIconWidth() + 3;
        }
        if (!StringUtil.isEmpty(comboBoxButton.getText())) {
          final Font font = comboBoxButton.getFont();
          g2.setFont(font);
          g2.setColor(textColor);
          g2.drawString(comboBoxButton.getText(), x, (size.height + font.getSize()) / 2 - 1);
        }
      }
      else {
        super.paint(g, c);
      }
    }
    final Insets insets = comboBoxButton.insets(); // FIXME [VISTALL] we need this ?
    final Icon icon = comboBoxButton.isEnabled() ? getArrowIcon() : getDisabledArrowIcon();
    final int x;
    if (isEmpty) {
      x = (size.width - icon.getIconWidth()) / 2;
    }
    else {
      if (comboBoxAction.isSmallVariant()) {
        x = size.width - icon.getIconWidth() - insets.right + 1;
      }
      else {
        x = size.width - icon.getIconWidth() - insets.right + (UIUtil.isUnderNimbusLookAndFeel() ? -3 : 2);
      }
    }

    icon.paintIcon(null, g, x, (size.height - icon.getIconHeight()) / 2);
    g.setPaintMode();
  }
}
