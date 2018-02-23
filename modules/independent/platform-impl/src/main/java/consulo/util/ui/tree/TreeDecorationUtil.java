/*
 * Copyright 2013-2017 consulo.io
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
package consulo.util.ui.tree;

import com.intellij.ide.ui.laf.darcula.DarculaUIUtil;
import com.intellij.ide.ui.laf.intellij.IntelliJLaf;
import com.intellij.ui.ColorUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.ui.laf.MorphColor;
import javax.annotation.Nonnull;

import javax.swing.*;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.basic.BasicTreeUI;
import java.awt.*;

/**
 * @author VISTALL
 * @since 19-Jun-17
 */
public class TreeDecorationUtil {
  public static void decorateTree(@Nonnull JTree tree) {
    TreeUI treeUI = tree.getUI();
    if (treeUI instanceof BasicTreeUI) {
      ((BasicTreeUI)treeUI).setLeftChildIndent(JBUI.scale(10));
    }

    tree.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER));
    tree.setBackground(getTreeBackground());
  }

  @Nonnull
  public static Color getTreeBackground() {
    return MorphColor.of(TreeDecorationUtil::calcColor);
  }

  @Nonnull
  private static Color calcColor() {
    if (UIUtil.isUnderAquaLookAndFeel()) {
      Color color = IntelliJLaf.isGraphite() ? DarculaUIUtil.MAC_GRAPHITE_COLOR : DarculaUIUtil.MAC_REGULAR_COLOR;
      return ColorUtil.desaturate(color, 8);
    }
    Color color = UIManager.getColor("Hyperlink.linkColor");
    if(color == null) {
      color = DarculaUIUtil.MAC_REGULAR_COLOR;
    }
    return UIUtil.isUnderDarkTheme()
           ? ColorUtil.darker(color, 10)
           : ColorUtil.desaturate(color, 18);
  }
}
