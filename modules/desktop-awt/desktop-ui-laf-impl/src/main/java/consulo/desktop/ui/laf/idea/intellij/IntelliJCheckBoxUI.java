// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.ui.laf.idea.intellij;

import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import consulo.desktop.ui.laf.idea.darcula.DarculaCheckBoxUI;

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
