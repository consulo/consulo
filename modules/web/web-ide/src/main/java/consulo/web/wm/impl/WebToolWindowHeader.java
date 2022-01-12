/*
 * Copyright 2013-2020 consulo.io
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
package consulo.web.wm.impl;

import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Size;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.style.ComponentColors;
import consulo.wm.impl.UnifiedToolWindowImpl;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-11-21
 */
public class WebToolWindowHeader {
  private DockLayout myDockLayout;

  @RequiredUIAccess
  public WebToolWindowHeader(UnifiedToolWindowImpl toolWindow) {
    myDockLayout = DockLayout.create();
    myDockLayout.setSize(new Size(-1, 24 + 4));
    //myDockLayout.addBorder(BorderPosition.TOP, BorderStyle.LINE, ComponentColors.BORDER, 1);
    myDockLayout.addBorder(BorderPosition.BOTTOM, BorderStyle.LINE, ComponentColors.BORDER, 1);

    HorizontalLayout titleLayout = HorizontalLayout.create();
    titleLayout.add(Label.create(toolWindow.getDisplayName()));

    myDockLayout.left(titleLayout);
  }

  @Nonnull
  public Component getComponent() {
    return myDockLayout;
  }
}
