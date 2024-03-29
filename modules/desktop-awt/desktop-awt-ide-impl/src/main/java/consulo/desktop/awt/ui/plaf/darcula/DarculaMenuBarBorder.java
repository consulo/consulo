// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.plaf.darcula;

import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBValue;

import javax.swing.border.Border;
import javax.swing.plaf.UIResource;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class DarculaMenuBarBorder implements Border, UIResource {
  private static final Color BORDER_COLOR = JBColor.namedColor("MenuBar.borderColor", new JBColor(Gray.xCD, Gray.x51));
  private static final JBValue BW = new JBValue.Float(1);

  @Override
  public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
    g.setColor(BORDER_COLOR);
    g.fillRect(x, y + h - BW.get(), w, BW.get());
  }

  @Override
  public Insets getBorderInsets(Component c) {
    return JBUI.insetsBottom(1).asUIResource();
  }

  @Override
  public boolean isBorderOpaque() {
    return true;
  }
}
