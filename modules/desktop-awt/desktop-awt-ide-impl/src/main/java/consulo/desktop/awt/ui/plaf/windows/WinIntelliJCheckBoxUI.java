// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.plaf.windows;

import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.EmptyIcon;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.desktop.awt.ui.plaf.darcula.DarculaCheckBoxUI;
import consulo.desktop.awt.ui.plaf.darcula.DarculaUIUtil;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * @author Konstantin Bulenkov
 */
public class WinIntelliJCheckBoxUI extends DarculaCheckBoxUI {
  private static final Icon DEFAULT_ICON = JBUI.scale(EmptyIcon.create(13)).asUIResource();

  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration"})
  public static ComponentUI createUI(JComponent c) {
    AbstractButton b = (AbstractButton)c;
    b.setRolloverEnabled(true);
    return new WinIntelliJCheckBoxUI();
  }

  @Override
  protected Rectangle updateViewRect(AbstractButton b, Rectangle viewRect) {
    JBInsets.removeFrom(viewRect, b.getInsets());
    return viewRect;
  }

  @Override
  protected Dimension computeOurPreferredSize(JComponent c) {
    return null;
  }

  @Override
  protected void drawCheckIcon(JComponent c, Graphics2D g, AbstractButton b, Rectangle iconRect, boolean selected, boolean enabled) {
    Graphics2D g2 = (Graphics2D)g.create();
    try {
      ButtonModel bm = b.getModel();

      String iconName = isIndeterminate(b) ? "checkBoxIndeterminate" : "checkBox";
      Object op = b.getClientProperty("JComponent.outline");
      boolean focused = op == null && c.hasFocus() || bm.isRollover() || isCellRollover(b);
      boolean pressed = bm.isPressed() || isCellPressed(b);
      Icon icon = WinIconLookup.INSTANCE.getIcon(iconName, selected || isIndeterminate(b), focused, enabled, false, pressed);
      icon.paintIcon(c, g, iconRect.x, iconRect.y);

      if (op != null) {
        DarculaUIUtil.Outline.valueOf(op.toString()).setGraphicsColor(g2, b.hasFocus());
        Path2D outline = new Path2D.Float(Path2D.WIND_EVEN_ODD);

        outline.append(new Rectangle2D.Float(iconRect.x - JBUIScale.scale(1), iconRect.y - JBUIScale.scale(1), JBUIScale.scale(15), JBUIScale.scale(15)), false);
        outline.append(new Rectangle2D.Float(iconRect.x + JBUIScale.scale(1), iconRect.y + JBUIScale.scale(1), JBUIScale.scale(11), JBUIScale.scale(11)), false);
        g2.fill(outline);
      }
    }
    finally {
      g2.dispose();
    }
  }

  private static boolean isCellRollover(AbstractButton checkBox) {
    Rectangle cellPosition = (Rectangle)checkBox.getClientProperty(UIUtil.CHECKBOX_ROLLOVER_PROPERTY);
    return cellPosition != null && cellPosition.getBounds().equals(checkBox.getBounds());
  }

  private static boolean isCellPressed(AbstractButton checkBox) {
    Rectangle cellPosition = (Rectangle)checkBox.getClientProperty(UIUtil.CHECKBOX_PRESSED_PROPERTY);
    return cellPosition != null && cellPosition.getBounds().equals(checkBox.getBounds());
  }

  @Override
  public Icon getDefaultIcon() {
    return DEFAULT_ICON;
  }

  @Override
  protected int textIconGap() {
    return JBUIScale.scale(4);
  }

  @Override
  protected int getMnemonicIndex(AbstractButton b) {
    return b.getDisplayedMnemonicIndex();
  }
}
