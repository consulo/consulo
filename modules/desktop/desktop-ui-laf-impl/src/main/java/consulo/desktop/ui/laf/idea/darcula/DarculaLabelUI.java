// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.ui.laf.idea.darcula;

import com.intellij.ide.ui.laf.darcula.DarculaLaf;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicLabelUI;
import java.awt.*;

public class DarculaLabelUI extends BasicLabelUI {
  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "unused"})
  public static ComponentUI createUI(JComponent c) {
    return new DarculaLabelUI();
  }

  @Override
  protected void paintEnabledText(JLabel l, Graphics g, String s, int textX, int textY) {
    g.setColor(l.getForeground());
    BasicGraphicsUtils.drawStringUnderlineCharAt(l, (Graphics2D)g, s, getMnemonicIndex(l), textX, textY);
  }

  @Override
  protected void paintDisabledText(JLabel l, Graphics g, String s, int textX, int textY) {
    g.setColor(UIManager.getColor("Label.disabledForeground"));
    BasicGraphicsUtils.drawStringUnderlineCharAt(l, (Graphics2D)g, s, -1, textX, textY);
  }

  protected int getMnemonicIndex(JLabel l) {
    return DarculaLaf.isAltPressed() ? l.getDisplayedMnemonicIndex() : -1;
  }
}
