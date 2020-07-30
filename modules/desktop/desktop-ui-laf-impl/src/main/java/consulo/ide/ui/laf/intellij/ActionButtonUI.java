/*
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
package consulo.ide.ui.laf.intellij;

import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.Gray;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import sun.swing.SwingUtilities2;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * @author VISTALL
 * @since 27-Nov-16.
 *
 * Code extract from {@link com.intellij.openapi.actionSystem.impl.ActionButton}
 */
public class ActionButtonUI extends ComponentUI implements consulo.actionSystem.impl.ActionButtonUI {
  private static final Color ALPHA_20 = Gray._0.withAlpha(20);
  private static final Color ALPHA_30 = Gray._0.withAlpha(30);
  private static final Color ALPHA_40 = Gray._0.withAlpha(40);
  private static final Color ALPHA_120 = Gray._0.withAlpha(120);
  private static final BasicStroke BASIC_STROKE = new BasicStroke();

  public static ActionButtonUI createUI(JComponent c) {
    return new ActionButtonUI();
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    if(c instanceof ActionButtonWithText) {
      paintTextButton(g, (ActionButtonWithText)c);
    }
    else {
      paintDefaultButton(g, (ActionButton)c);
    }
  }

  private void paintTextButton(Graphics g, ActionButtonWithText c) {
    AnAction action = c.getAction();

    Icon icon = c.getIcon();
    FontMetrics fm = SwingUtilities2.getFontMetrics(c, g, c.getFont());
    Rectangle viewRect = new Rectangle(c.getSize());
    Insets i = c.getInsets();
    viewRect.x += i.left;
    viewRect.y += i.top;
    viewRect.width -= (i.right + viewRect.x);
    viewRect.height -= (i.bottom + viewRect.y);

    Rectangle iconRect = new Rectangle();
    Rectangle textRect = new Rectangle();
    String text = SwingUtilities
            .layoutCompoundLabel(c, fm, c.getText(), icon, SwingConstants.CENTER, c.horizontalTextAlignment(), SwingConstants.CENTER,
                                 SwingConstants.TRAILING, viewRect, iconRect, textRect, c.iconTextSpace());
    int state = c.getPopState();

    if (state != ActionButtonComponent.NORMAL) {
      paintBackground(c, g, c.getSize(), state);
    }

    icon.paintIcon(null, g, iconRect.x, iconRect.y);

    UISettings.setupAntialiasing(g);
    g.setColor(c.isButtonEnabled() ? c.getForeground() : UIUtil.getInactiveTextColor());
    SwingUtilities2.drawStringUnderlineCharAt(c, g, text, getMnemonicCharIndex(c, action, text), textRect.x, textRect.y + fm.getAscent());
  }

  private int getMnemonicCharIndex(ActionButton button, AnAction action, String text) {
    final int mnemonicIndex = button.getDisplayedMnemonicIndex();
    if (mnemonicIndex != -1) {
      return mnemonicIndex;
    }
    final ShortcutSet shortcutSet = action.getShortcutSet();
    final Shortcut[] shortcuts = shortcutSet.getShortcuts();
    for (int i = 0; i < shortcuts.length; i++) {
      Shortcut shortcut = shortcuts[i];
      if (shortcut instanceof KeyboardShortcut) {
        KeyboardShortcut keyboardShortcut = (KeyboardShortcut)shortcut;
        if (keyboardShortcut.getSecondKeyStroke() == null) { // we are interested only in "mnemonic-like" shortcuts
          final KeyStroke keyStroke = keyboardShortcut.getFirstKeyStroke();
          final int modifiers = keyStroke.getModifiers();
          if ((modifiers & KeyEvent.ALT_MASK) != 0) {
            return (keyStroke.getKeyChar() != KeyEvent.CHAR_UNDEFINED)
                   ? text.indexOf(keyStroke.getKeyChar())
                   : text.indexOf(KeyEvent.getKeyText(keyStroke.getKeyCode()));
          }
        }
      }
    }
    return -1;
  }
  private void paintDefaultButton(Graphics g, ActionButton c) {
    int state = c.getPopState();

    paintBackground(c, g, c.getSize(), state);
    if(!c.isWithoutBorder()) {
      paintBorder(c, g, c.getSize(), state);
    }
    paintIcon(g, c, c.getIcon());

    if (c.shallPaintDownArrow()) {
      int x = JBUI.scale(5);
      int y = JBUI.scale(4);

      if (state == ActionButtonComponent.PUSHED) {
        x += JBUI.scale(1);
        y += JBUI.scale(1);
      }

      AllIcons.General.Dropdown.paintIcon(c, g, x, y);
    }
  }

  public void paintBorder(ActionButton button, Graphics g, Dimension size, int state) {
    if (state == ActionButtonComponent.NORMAL && !button.isBackgroundSet()) return;

    if (UIUtil.isUnderAquaLookAndFeel()) {
      if (state == ActionButtonComponent.POPPED) {
        g.setColor(ALPHA_30);
        g.drawRoundRect(0, 0, size.width - 2, size.height - 2, 4, 4);
      }
    }
    else {
      final double shift = UIUtil.isUnderDarcula() ? 1 / 0.49 : 0.49;
      g.setColor(ColorUtil.shift(UIUtil.getPanelBackground(), shift));
      ((Graphics2D)g).setStroke(BASIC_STROKE);
      final GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
      g.drawRoundRect(0, 0, size.width - JBUI.scale(2), size.height - JBUI.scale(2), JBUI.scale(4), JBUI.scale(4));
      config.restore();
    }
  }

  public void paintBackground(ActionButton button, Graphics g, Dimension size, int state) {
    if (state == ActionButtonComponent.NORMAL && !button.isBackgroundSet()) return;

    if (UIUtil.isUnderAquaLookAndFeel()) {
      if (state == ActionButtonComponent.PUSHED) {
        ((Graphics2D)g).setPaint(UIUtil.getGradientPaint(0, 0, ALPHA_40, size.width, size.height, ALPHA_20));
        g.fillRect(0, 0, size.width - 1, size.height - 1);

        g.setColor(ALPHA_120);
        g.drawLine(0, 0, 0, size.height - 2);
        g.drawLine(1, 0, size.width - 2, 0);

        g.setColor(ALPHA_30);
        g.drawRect(1, 1, size.width - 3, size.height - 3);
      }
      else if (state == ActionButtonComponent.POPPED) {
        ((Graphics2D)g).setPaint(UIUtil.getGradientPaint(0, 0, Gray._235, 0, size.height, Gray._200));
        g.fillRect(1, 1, size.width - 3, size.height - 3);
      }
    }
    else {
      final Color bg = UIUtil.getPanelBackground();
      final boolean dark = UIUtil.isUnderDarcula();
      g.setColor(state == ActionButtonComponent.PUSHED ? ColorUtil.shift(bg, dark ? 1d / 0.7d : 0.7d) : dark ? Gray._255.withAlpha(40) : ALPHA_40);
      g.fillRect(JBUI.scale(1), JBUI.scale(1), size.width - JBUI.scale(2), size.height - JBUI.scale(2));
    }
  }

  public void paintIcon(Graphics g, ActionButtonComponent actionButton, Icon icon) {
    final int width = icon.getIconWidth();
    final int height = icon.getIconHeight();
    final int x = (int)Math.ceil((actionButton.getWidth() - width) / 2);
    final int y = (int)Math.ceil((actionButton.getHeight() - height) / 2);

    icon.paintIcon(null, g, x, y);
  }
}