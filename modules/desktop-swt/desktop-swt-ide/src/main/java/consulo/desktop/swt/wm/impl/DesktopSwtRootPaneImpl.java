/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.wm.impl;

import com.intellij.openapi.wm.StatusBar;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 12/12/2021
 */
public class DesktopSwtRootPaneImpl {
  private DockLayout myDockLayout = DockLayout.create();

  public void setSizeFull() {
   // TargetVaddin.to(myDockLayout).setSizeFull();
  }

  @RequiredUIAccess
  public void setCenterComponent(@Nullable Component centerComponent) {
    myDockLayout.center(centerComponent);
  }

  @RequiredUIAccess
  public void setMenuBar(@Nullable MenuBar menuBar) {
    myDockLayout.top(menuBar);
  }

  @Nonnull
  public Component getComponent() {
    return myDockLayout;
  }

  @RequiredUIAccess
  public void setStatusBar(StatusBar statusBar) {
    myDockLayout.bottom(statusBar.getUIComponent());
  }
}
