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
package consulo.util.ui;

import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.ui.decorator.SwingUIDecorator;

import java.awt.*;

/**
 * @author VISTALL
 * @since 22-Jun-17
 */
public class ToolwindowPaintUtil {
  public static void drawHeader(Graphics g, int x, int width, int height, boolean active, boolean drawTopLine) {
    drawHeader(g, x, width, height, active, false, drawTopLine, true);
  }

  public static void drawHeader(Graphics g, int x, int width, int height, boolean active, boolean toolWindow, boolean drawTopLine, boolean drawBottomLine) {
    g.setColor(UIUtil.getPanelBackground());
    g.fillRect(x, 0, width, height);

    if (active) {
      g.setColor(SwingUIDecorator.get(SwingUIDecorator::getSidebarColor));
      g.fillRect(x, 0, width, height);
    }

    g.setColor(UIUtil.getBorderColor());
    if (drawTopLine) g.drawLine(x, 0, width, 0);
    if (drawBottomLine) g.drawLine(x, height - JBUI.scale(1), width, height - JBUI.scale(1));
  }
}
