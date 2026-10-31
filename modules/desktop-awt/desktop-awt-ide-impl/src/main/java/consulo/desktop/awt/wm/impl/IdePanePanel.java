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
package consulo.desktop.awt.wm.impl;

import consulo.desktop.awt.ui.impl.plaf.BasicIdePanePanelUI;
import consulo.ui.ex.awt.JBPanel;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.ComponentColors;

import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.Objects;

public class IdePanePanel extends JBPanel {
  public IdePanePanel(LayoutManager layout) {
    this(layout, true);
  }

  protected IdePanePanel(LayoutManager layout, boolean editorAreaBackground) {
    super(layout);

    if (editorAreaBackground) {
      setBackground(TargetAWT.to(ComponentColors.EDITOR_AREA_BACKGROUND));
    }
  }

  public void setIslandRole(@Nullable String role) {
    if (Objects.equals(getClientProperty(BasicIdePanePanelUI.ISLAND_ROLE), role)) {
      return;
    }
    putClientProperty(BasicIdePanePanelUI.ISLAND_ROLE, role);
    updateUI();
    revalidate();
    repaint();
  }

  @Override
  public String getUIClassID() {
    return BasicIdePanePanelUI.UI_CLASS_ID;
  }

  @Override
  protected void paintChildren(Graphics g) {
    super.paintChildren(g);

    if (ui instanceof BasicIdePanePanelUI paneUI) {
      paneUI.paintAfterChildren(this, g);
    }
  }

  @Override
  public boolean isPaintingOrigin() {
    return ui instanceof BasicIdePanePanelUI paneUI && paneUI.isPaintingOrigin(this);
  }
}
