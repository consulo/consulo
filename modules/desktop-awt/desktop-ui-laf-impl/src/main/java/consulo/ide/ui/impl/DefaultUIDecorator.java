/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ide.ui.impl;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.ColorUtil;
import com.intellij.util.ui.JBUI;
import consulo.desktop.util.awt.MorphColor;
import consulo.desktop.util.awt.laf.BuildInLookAndFeel;
import consulo.ide.ui.impl.mac.MacAquaUIDecorator;
import consulo.ui.decorator.SwingUIDecorator;
import consulo.ui.style.StyleManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.basic.BasicTreeUI;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2018-07-23
 */
public class DefaultUIDecorator implements SwingUIDecorator {
  @Override
  public boolean isAvaliable() {
    // default decorator
    return true;
  }

  @Nullable
  @Override
  public Color getSidebarColor() {
    return MorphColor.of(this::calcSidebarColor);
  }

  @Override
  public boolean isDark() {
    LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
    return lookAndFeel instanceof BuildInLookAndFeel && ((BuildInLookAndFeel)lookAndFeel).isDark();
  }

  @Nonnull
  private Color calcSidebarColor() {
    Color color = UIManager.getColor("Hyperlink.linkColor");
    if (color == null) {
      color = MacAquaUIDecorator.MAC_REGULAR_COLOR;
    }
    return StyleManager.get().getCurrentStyle().isDark() ? ColorUtil.darker(color, 10) : ColorUtil.desaturate(color, 18);
  }

  @Override
  public boolean decorateSidebarTree(@Nonnull JTree tree) {
    decorateTree0(tree, getSidebarColor());
    return true;
  }

  /**
   * Enable & disable macOS dark title decoration. Works only on JetBrains JRE
   * <p/>
   * https://github.com/JetBrains/jdk8u_jdk/commit/83e6b1c2e67a192558f8882f663718d4bea0c8b0
   */
  @Override
  public boolean decorateWindowTitle(@Nonnull JRootPane rootPane) {
    if(SystemInfo.isMac) {
      rootPane.putClientProperty("jetbrains.awt.windowDarkAppearance", StyleManager.get().getCurrentStyle().isDark());
    }
    return true;
  }

  public static void decorateTree0(JTree tree, Color color) {
    TreeUI treeUI = tree.getUI();
    if (treeUI instanceof BasicTreeUI) {
      ((BasicTreeUI)treeUI).setLeftChildIndent(JBUI.scale(10));
    }

    tree.setFont(JBUI.Fonts.biggerFont());
    tree.setBackground(color);
  }
}
