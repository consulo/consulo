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
package com.intellij.openapi.wm.impl;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.ui.Gray;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.metal.MetalToggleButtonUI;
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * @author Vladimir Kondratyev
 */
public final class DesktopStripeButtonUI extends MetalToggleButtonUI {
  private final Rectangle myIconRect = new Rectangle();
  private final Rectangle myTextRect = new Rectangle();
  private final Rectangle myViewRect = new Rectangle();
  private Insets myViewInsets = new Insets(0, 0, 0, 0);

  private DesktopStripeButtonUI() {
  }

  /**
   * Invoked by reflection
   */
  public static ComponentUI createUI(final JComponent c) {
    return new DesktopStripeButtonUI();
  }

  @Override
  public Dimension getPreferredSize(final JComponent c) {
    final AnchoredButton button = (AnchoredButton)c;
    final Dimension dim = super.getPreferredSize(button);

    dim.width = (int)(JBUI.scale(4) + dim.width * 1.1f);
    dim.height += JBUI.scale(2);

    final ToolWindowAnchor anchor = button.getAnchor();
    if (ToolWindowAnchor.LEFT == anchor || ToolWindowAnchor.RIGHT == anchor) {
      return new Dimension(dim.height, dim.width);
    }
    else {
      return dim;
    }
  }

  @Override
  public void paint(final Graphics g, final JComponent c) {
    final AnchoredButton button = (AnchoredButton)c;

    final String text = button.getText();
    final Icon icon = (button.isEnabled()) ? button.getIcon() : button.getDisabledIcon();

    if (icon == null && text == null) {
      return;
    }

    final FontMetrics fm = button.getFontMetrics(button.getFont());
    myViewInsets = c.getInsets(myViewInsets);

    myViewRect.x = myViewInsets.left;
    myViewRect.y = myViewInsets.top;

    final ToolWindowAnchor anchor = button.getAnchor();

    // Use inverted height & width
    if (ToolWindowAnchor.RIGHT == anchor || ToolWindowAnchor.LEFT == anchor) {
      myViewRect.height = c.getWidth() - (myViewInsets.left + myViewInsets.right);
      myViewRect.width = c.getHeight() - (myViewInsets.top + myViewInsets.bottom);
    }
    else {
      myViewRect.height = c.getHeight() - (myViewInsets.left + myViewInsets.right);
      myViewRect.width = c.getWidth() - (myViewInsets.top + myViewInsets.bottom);
    }

    myIconRect.x = myIconRect.y = myIconRect.width = myIconRect.height = 0;
    myTextRect.x = myTextRect.y = myTextRect.width = myTextRect.height = 0;

    final String clippedText = SwingUtilities
            .layoutCompoundLabel(c, fm, text, icon, button.getVerticalAlignment(), button.getHorizontalAlignment(),
                                 button.getVerticalTextPosition(), button.getHorizontalTextPosition(), myViewRect,
                                 myIconRect, myTextRect, button.getText() == null ? 0 : button.getIconTextGap());

    // Paint button's background

    final Graphics2D g2 = (Graphics2D)g.create();

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    final ButtonModel model = button.getModel();
    final Color background = button.getBackground();
    myIconRect.x -= JBUI.scale(2);
    myTextRect.x -= JBUI.scale(2);
    final int off = JBUI.scale(1);
    if (model.isArmed() && model.isPressed() || model.isSelected() || model.isRollover()) {
      if (anchor.isHorizontal()) g2.translate(0, -off);
      final boolean dark = UIUtil.isUnderDarkBuildInLaf();
      g2.setColor(dark
                  ? Gray._15.withAlpha(model.isSelected() ? 85 : 40)
                  : Gray._85.withAlpha(model.isSelected() ? 85 : 40));
      g2.fillRect(0, 0, button.getWidth(), button.getHeight());
      if (anchor.isHorizontal()) g2.translate(0, off);
    }


    AffineTransform tr = null;
    if (ToolWindowAnchor.RIGHT == anchor || ToolWindowAnchor.LEFT == anchor) {
      tr = g2.getTransform();
      if (ToolWindowAnchor.RIGHT == anchor) {
        if (icon != null) { // do not rotate icon
          icon.paintIcon(c, g2, myIconRect.y, myIconRect.x);
        }
        g2.rotate(Math.PI / 2);
        g2.translate(0, -c.getWidth());
      }
      else {
        if (icon != null) { // do not rotate icon
          icon.paintIcon(c, g2, myIconRect.y, c.getHeight() - myIconRect.x - icon.getIconHeight());
        }
        g2.rotate(-Math.PI / 2);
        g2.translate(-c.getHeight(), 0);
      }
    }
    else {
      if (icon != null) {
        icon.paintIcon(c, g2, myIconRect.x, myIconRect.y);
      }
    }

    // paint text

    UISettings.setupAntialiasing(g2);
    if (text != null) {
      if (model.isEnabled()) {
        if (model.isArmed() && model.isPressed() || model.isSelected()) {
          g2.setColor(background);
        }
        else {
          g2.setColor(button.getForeground());
        }
      }
      else {
        g2.setColor(background.darker());
      }
      /* Draw the Text */
      if (model.isEnabled()) {
        /*** paint the text normally */
        g2.setColor(UIUtil.isUnderDarkTheme() && model.isSelected()
                    ? button.getForeground().brighter()
                    : button.getForeground());
        BasicGraphicsUtils
                .drawString(g2, clippedText, button.getMnemonic2(), myTextRect.x, myTextRect.y + fm.getAscent());
      }
      else {
        /*** paint the text disabled ***/
        if (model.isSelected()) {
          g2.setColor(c.getBackground());
        }
        else {
          g2.setColor(getDisabledTextColor());
        }
        BasicGraphicsUtils
                .drawString(g2, clippedText, button.getMnemonic2(), myTextRect.x, myTextRect.y + fm.getAscent());
      }
    }
    if (ToolWindowAnchor.RIGHT == anchor || ToolWindowAnchor.LEFT == anchor) {
      g2.setTransform(tr);
    }

    g2.dispose();
  }
}
