// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.plaf.windows;

import consulo.ui.ex.awt.EmptyIcon;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.JBUI;
import consulo.desktop.awt.ui.plaf.darcula.DarculaRadioButtonUI;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class WinIntelliJRadioButtonUI extends DarculaRadioButtonUI {
  private static final Icon DEFAULT_ICON = JBUI.scale(EmptyIcon.create(13)).asUIResource();

  @Override
  protected Rectangle updateViewRect(AbstractButton b, Rectangle viewRect) {
    JBInsets.removeFrom(viewRect, b.getInsets());
    return viewRect;
  }

  @Override
  protected Dimension computeOurPreferredSize(JComponent c) {
    return null;
  }

  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration"})
  public static ComponentUI createUI(JComponent c) {
    AbstractButton b = (AbstractButton)c;
    b.setRolloverEnabled(true);
    return new WinIntelliJRadioButtonUI();
  }

  @Override
  protected void paintIcon(JComponent c, Graphics2D g, Rectangle viewRect, Rectangle iconRect) {
    AbstractButton b = (AbstractButton)c;
    ButtonModel bm = b.getModel();
    boolean focused = c.hasFocus() || bm.isRollover();
    Icon icon = WinIconLookup.INSTANCE.getIcon("radio", bm.isSelected(), focused, bm.isEnabled(), false, bm.isPressed());
    icon.paintIcon(c, g, iconRect.x, iconRect.y);
  }

  @Override
  public Icon getDefaultIcon() {
    return DEFAULT_ICON;
  }

  @Override
  protected int getMnemonicIndex(AbstractButton b) {
    return b.getDisplayedMnemonicIndex();
  }
}
