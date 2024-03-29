// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.plaf.intellij;

import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.EmptyIcon;
import consulo.ui.ex.awt.JBUI;
import consulo.desktop.awt.ui.plaf.darcula.DarculaCheckBoxUI;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;

public class IntelliJCheckBoxUI extends DarculaCheckBoxUI {
  private static final Icon DEFAULT_ICON = JBUI.scale(EmptyIcon.create(19));

  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration"})
  public static ComponentUI createUI(JComponent c) {
    return new IntelliJCheckBoxUI();
  }

  @Override
  public Icon getDefaultIcon() {
    return DEFAULT_ICON;
  }

  @Override
  protected int textIconGap() {
    return JBUIScale.scale(4);
  }
}
