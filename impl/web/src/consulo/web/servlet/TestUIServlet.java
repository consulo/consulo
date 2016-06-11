/*
 * Copyright 2013-2016 must-be.org
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
package consulo.web.servlet;

import consulo.ui.*;
import consulo.ui.layout.DockLayout;
import consulo.web.servlet.ui.UIRoot;
import consulo.web.servlet.ui.UIServlet;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class TestUIServlet extends UIServlet {
  public TestUIServlet() {
    super("ui");
  }

  @NotNull
  @Override
  public UIRoot createUIRoot() {
    return new UIRoot() {
      @NotNull
      @Override
      public Component create(@NotNull UIAccess uiAccess) {
        DockLayout dockLayout = UIFactory.Layouts.dock();

        dockLayout.top(create("top"));
        dockLayout.left(create("left"));
        dockLayout.right(create("right"));
        dockLayout.bottom(create("bottom"));
        dockLayout.center(create("center"));
        return dockLayout;
      }

      private CheckBox create(String text) {
        CheckBox checkBox = UIFactory.Components.checkBox("UI proxy?=" + text, true);
        checkBox.addSelectListener(new CheckBox.SelectListener() {
          @Override
          @RequiredUIThread
          public void selectChanged(@NotNull CheckBox checkBox) {
            // swing api start
            try {
              System.out.println("Test ME " + checkBox.isSelected() + " " + UIAccess.isUIThread());
            }
            catch (Exception e) {
              e.printStackTrace();
            }
            // swing api stop
          }
        });
        return checkBox;
      }
    };
  }
}
