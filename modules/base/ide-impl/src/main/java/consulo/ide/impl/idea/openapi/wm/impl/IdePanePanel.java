/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.wm.impl;

import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBPanel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.style.StyleManager;

import java.awt.*;

public class IdePanePanel extends JBPanel {
  public IdePanePanel(LayoutManager layout) {
    super(layout);

    setBackground(new JBColor(() -> {
      Color light = ColorUtil.darker(UIUtil.getPanelBackground(), 3);
      return StyleManager.get().getCurrentStyle().isDark() ? Gray._40 : light;
    }));
  }
}
