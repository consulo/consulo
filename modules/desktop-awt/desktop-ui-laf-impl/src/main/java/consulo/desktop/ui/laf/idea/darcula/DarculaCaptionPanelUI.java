/*
 * Copyright 2013-2019 consulo.io
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

import com.intellij.ui.CaptionPanel;
import com.intellij.util.ui.JBUI;
import consulo.desktop.ui.laf.ui.basic.BasicCaptionPanelUI;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2019-11-05
 */
public class DarculaCaptionPanelUI extends BasicCaptionPanelUI {
  public static ComponentUI createUI(JComponent c) {
    return new DarculaCaptionPanelUI();
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    super.paint(g, c);

    final Graphics2D g2d = (Graphics2D)g;
    CaptionPanel panel = (CaptionPanel)c;

    g2d.setPaint(JBUI.CurrentTheme.Popup.headerBackground(panel.isActive()));
    g2d.fillRect(0, 0, panel.getWidth(), panel.getHeight());
  }
}
