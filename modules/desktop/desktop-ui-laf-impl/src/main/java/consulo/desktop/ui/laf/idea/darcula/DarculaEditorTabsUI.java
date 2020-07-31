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
package consulo.desktop.ui.laf.idea.darcula;

import com.intellij.ui.Gray;
import com.intellij.util.ui.UIUtil;
import consulo.ide.ui.laf.JBEditorTabsUI;
import consulo.ide.ui.laf.mac.MacEditorTabsUI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 15-Jun-17
 */
public class DarculaEditorTabsUI extends MacEditorTabsUI {
  public static JBEditorTabsUI createUI(JComponent c) {
    return new DarculaEditorTabsUI();
  }

  @Override
  public void doPaintInactiveImpl(Graphics2D g2d, Rectangle effectiveBounds, int x, int y, int w, int h, Color tabColor, int row, int column, boolean vertical) {
    if (tabColor != null) {
      g2d.setColor(tabColor);
      g2d.fillRect(x, y, w, h);
    }
    else {
      g2d.setPaint(UIUtil.getControlColor());
      g2d.fillRect(x, y, w, h);
    }

    g2d.setColor(Gray._0.withAlpha(10));
    g2d.drawRect(x, y, w - 1, h - 1);
  }

  @Nonnull
  @Override
  protected Color prepareColorForTab(@Nullable Color c) {
    if(c == null) {
      return Gray._60;
    }
    return c;
  }

  @Override
  public Color getBackground() {
    return new Color(0x3C3F41);
  }
}
