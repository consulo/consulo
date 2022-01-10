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
package consulo.ide.ui.impl.mac;

import com.intellij.ui.ColorUtil;
import com.intellij.util.ui.UIUtil;
import consulo.ide.ui.impl.DefaultUIDecorator;
import consulo.ui.decorator.SwingUIDecorator;
import consulo.ui.style.StyleManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2018-07-23
 */
public class MacAquaUIDecorator implements SwingUIDecorator {
  public static final Color MAC_REGULAR_COLOR = new Color(0x80479cfc, true);

  public static final Color MAC_GRAPHITE_COLOR = new Color(0x8099979d, true);

  @Override
  public boolean decorateSidebarTree(@Nonnull JTree tree) {
    DefaultUIDecorator.decorateTree0(tree, getSidebarColor());
    return true;
  }

  /**
   * Enable & disable macOS dark title decoration. Works only on JetBrains JRE
   * <p/>
   * https://github.com/JetBrains/jdk8u_jdk/commit/83e6b1c2e67a192558f8882f663718d4bea0c8b0
   */
  @Override
  public boolean decorateWindowTitle(@Nonnull JRootPane rootPane) {
    rootPane.putClientProperty("jetbrains.awt.windowDarkAppearance", StyleManager.get().getCurrentStyle().isDark());
    return true;
  }

  @Nullable
  @Override
  public Color getSidebarColor() {
    Color color = UIUtil.isGraphite() ? MAC_GRAPHITE_COLOR : MAC_REGULAR_COLOR;

    return ColorUtil.desaturate(color, 8);
  }

  @Override
  public boolean isAvaliable() {
    LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
    return "com.apple.laf.AquaLookAndFeel".equals(lookAndFeel.getClass().getName());
  }
}
