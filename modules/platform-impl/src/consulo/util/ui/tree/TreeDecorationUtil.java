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

import com.intellij.ui.ColorUtil;
import com.intellij.util.ui.UIUtil;
import consulo.ui.laf.MorphColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.basic.BasicTreeUI;
import java.awt.*;

/**
 * @author VISTALL
 * @since 19-Jun-17
 */
public class TreeDecorationUtil {
  public static void decorateTree(@NotNull JTree tree) {
    TreeUI treeUI = tree.getUI();
    if (treeUI instanceof BasicTreeUI) {
      ((BasicTreeUI)treeUI).setLeftChildIndent(10);
    }

    tree.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER));
    tree.setBackground(getTreeBackground());
  }

  @NotNull
  public static Color getTreeBackground() {
    return MorphColor.of(() -> UIUtil.isUnderDarkTheme()
                               ? ColorUtil.darker(UIManager.getColor("Hyperlink.linkColor"), 10)
                               : ColorUtil.desaturate(UIManager.getColor("Hyperlink.linkColor"), 18));
  }
}
