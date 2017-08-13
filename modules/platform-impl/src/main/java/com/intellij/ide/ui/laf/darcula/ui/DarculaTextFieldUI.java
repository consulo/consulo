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
package com.intellij.ide.ui.laf.darcula.ui;

import com.intellij.ide.ui.laf.darcula.DarculaUIUtil;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.Gray;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;

/**
 * @author Konstantin Bulenkov
 */
public class DarculaTextFieldUI extends BasicTextFieldUI {
  private static final Icon SEARCH_ICON = IconLoader.findIcon("/com/intellij/ide/ui/laf/icons/search.png");
  private static final Icon SEARCH_WITH_HISTORY_ICON =
          IconLoader.findIcon("/com/intellij/ide/ui/laf/icons/searchWithHistory.png");
  private static final Icon CLEAR_ICON = IconLoader.findIcon("/com/intellij/ide/ui/laf/icons/clear.png");

  private enum SearchAction {POPUP, CLEAR}

  private final JTextField myTextField;
  protected JLabel myClearIcon;
  protected JLabel myRecentIcon;

  public DarculaTextFieldUI(JTextField textField) {
    myTextField = textField;
  }

  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  public static ComponentUI createUI(final JComponent c) {
    final DarculaTextFieldUI ui = new DarculaTextFieldUI((JTextField)c);
    c.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        c.repaint();
      }

      @Override
      public void focusLost(FocusEvent e) {
        c.repaint();
      }
    });
    c.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        if (ui.getComponent() != null && isSearchField(c)) {
          if (ui.getActionUnder(e) != null) {
            c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          }
          else {
            c.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
          }
        }
      }
    });
    c.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (isSearchField(c)) {
          final SearchAction action = ui.getActionUnder(e);
          if (action != null) {
            switch (action) {
              case POPUP:
                ui.showSearchPopup();
                break;
              case CLEAR:
                ((JTextField)c).setText("");
                break;
            }
            e.consume();
          }
        }
      }
    });
    return ui;
  }

  protected void showSearchPopup() {
    final Object value = getComponent().getClientProperty("JTextField.Search.FindPopup");
    if (value instanceof JPopupMenu) {
      final JPopupMenu popup = (JPopupMenu)value;
      popup.show(getComponent(), getSearchIconCoord().x, getComponent().getHeight());
    }
  }

  private SearchAction getActionUnder(MouseEvent e) {
    final Point cPoint = getClearIconCoord();
    final Point sPoint = getSearchIconCoord();
    cPoint.x += 8;
    cPoint.y += 8;
    sPoint.x += 8;
    sPoint.y += 8;
    final Point ePoint = e.getPoint();
    return cPoint.distance(ePoint) <= 8 ? SearchAction.CLEAR : sPoint.distance(ePoint) <= 8 ? SearchAction.POPUP : null;
  }

  protected Rectangle getDrawingRect() {
    final JTextComponent c = myTextField;
    final Insets i = c.getInsets();
    final int x = i.right - JBUI.scale(4) - JBUI.scale(16);
    final int y = i.top - JBUI.scale(3);
    final int w = c.getWidth() - (i.right + i.left) + JBUI.scale(16 * 2) + JBUI.scale(7 * 2) - JBUI.scale(5);
    int h = c.getBounds().height - (i.top + i.bottom) + JBUI.scale(4 * 2) - JBUI.scale(3);
    if (h % 2 == 1) h += JBUI.scale(1);
    return new Rectangle(x, y, w, h);
  }

  protected Point getSearchIconCoord() {
    final Rectangle r = getDrawingRect();
    return new Point(r.x + JBUI.scale(3), r.y + (r.height - JBUI.scale(16)) / 2 + JBUI.scale(1));
  }

  protected Point getClearIconCoord() {
    final Rectangle r = getDrawingRect();
    return new Point(r.x + r.width - JBUI.scale(16 - 1), r.y + (r.height - JBUI.scale(16)) / 2);
  }

  @Override
  protected void paintBackground(Graphics graphics) {
    Graphics2D g = (Graphics2D)graphics;
    final JTextComponent c = getComponent();
    final Container parent = c.getParent();
    final Rectangle r = getDrawingRect();
    if (c.isOpaque() && parent != null) {
      g.setColor(parent.getBackground());
      g.fillRect(0, 0, c.getWidth(), c.getHeight());
    }
    final GraphicsConfig config = new GraphicsConfig(g);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

    final Border border = c.getBorder();
    if (isSearchField(c)) {
      g.setColor(c.getBackground());

      int radius = r.height - JBUI.scale(1);
      g.fillRoundRect(r.x, r.y, r.width, r.height - JBUI.scale(1), radius, radius);
      g.setColor(c.isEnabled() ? Gray._100 : new Color(0x535353));
      if (c.hasFocus() && c.getClientProperty("JTextField.Search.noFocusRing") != Boolean.TRUE) {
        DarculaUIUtil.paintSearchFocusRing(g, r, c);
      }
      else {
        g.drawRoundRect(r.x, r.y, r.width, r.height - 1, radius, radius);
      }
      Point p = getSearchIconCoord();
      Icon searchIcon = getComponent().getClientProperty("JTextField.Search.FindPopup") instanceof JPopupMenu
                        ? SEARCH_WITH_HISTORY_ICON
                        : SEARCH_ICON;
      searchIcon.paintIcon(null, g, p.x, p.y);
      if (getComponent().hasFocus() && getComponent().getText().length() > 0) {
        p = getClearIconCoord();
        CLEAR_ICON.paintIcon(null, g, p.x, p.y);
      }
    }
    else if (border instanceof DarculaTextBorder) {
      if (c.isEnabled() && c.isEditable()) {
        g.setColor(c.getBackground());
      }
      final int width = c.getWidth();
      final int height = c.getHeight();
      final Insets i = border.getBorderInsets(c);
      if (c.hasFocus()) {
        g.fillRoundRect(i.left - JBUI.scale(5), i.top - JBUI.scale(2), width - i.right - i.left + JBUI.scale(10),
                        height - i.top - i.bottom + JBUI.scale(6), JBUI.scale(5), JBUI.scale(5));
      }
      else {
        g.fillRect(i.left - JBUI.scale(5), i.top - JBUI.scale(2), width - i.right - i.left + JBUI.scale(12),
                   height - i.top - i.bottom + 6);
      }
    }
    else {
      super.paintBackground(g);
    }
    config.restore();
  }

  @Override
  protected void paintSafely(Graphics g) {
    paintBackground(g);
    super.paintSafely(g);
  }

  public static boolean isSearchField(Component c) {
    return c instanceof JTextField && "search".equals(((JTextField)c).getClientProperty("JTextField.variant"));
  }

  public static boolean isSearchFieldWithHistoryPopup(Component c) {
    return isSearchField(c) && ((JTextField)c).getClientProperty("JTextField.Search.FindPopup") instanceof JPopupMenu;
  }
}
