/*
 * Copyright 2013-2023 consulo.io
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
package consulo.web.internal.ui;

import consulo.ide.impl.wm.impl.UnifiedStatusBarImpl;
import consulo.ui.MenuBar;
import consulo.ui.layout.DockLayout;

/**
 * @author VISTALL
 * @since 27/05/2023
 */
public class WebRootPaneImpl  {
  private final DockLayout myDockLayout = DockLayout.create();

  public WebRootPaneImpl() {
  }

  public void setCenterComponent(consulo.ui.Component content) {
    myDockLayout.center(content);
  }

  public void setMenuBar(MenuBar menuBar) {
    myDockLayout.top(menuBar);
  }

  public void setStatusBar(UnifiedStatusBarImpl statusBar) {
    myDockLayout.bottom(statusBar.getUIComponent());
  }

  public consulo.ui.Component getComponent() {
    return myDockLayout;
  }
}
