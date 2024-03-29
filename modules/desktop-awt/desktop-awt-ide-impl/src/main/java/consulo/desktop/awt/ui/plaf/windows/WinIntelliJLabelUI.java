// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.plaf.windows;

import consulo.desktop.awt.ui.plaf.darcula.DarculaLabelUI;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;

public class WinIntelliJLabelUI extends DarculaLabelUI {
  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "unused"})
  public static ComponentUI createUI(JComponent c) {
    return new WinIntelliJLabelUI();
  }

  @Override
  protected int getMnemonicIndex(JLabel l) {
    return l.getDisplayedMnemonicIndex();
  }
}
