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
package consulo.ide.ui.laf;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuBarUI;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2018-07-30
 */
public class DefaultBasicMenuBarUI extends BasicMenuBarUI {
  public static ComponentUI createUI(JComponent c) {
    return new DefaultBasicMenuBarUI();
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    if(c.isOpaque()) {
      Rectangle bounds = c.getBounds();
      g.fillRect(0, 0, bounds.width, bounds.height);
    }
  }
}
