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
package consulo.sandboxPlugin.ui;

import consulo.ide.ui.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.ui.Alerts;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.app.WindowWrapper;
import consulo.ui.layout.*;
import consulo.ui.shared.Size;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2020-05-29
 */
public class UITester {
  private static class MyWindowWrapper extends WindowWrapper {

    public MyWindowWrapper() {
      super("UI Tester");
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    protected Component createCenterComponent() {
      TabbedLayout tabbedLayout = TabbedLayout.create();

      tabbedLayout.addTab("Layouts", layouts());
      tabbedLayout.addTab("Components", components());

      return tabbedLayout;
    }

    @RequiredUIAccess
    private Component layouts() {
      TabbedLayout tabbedLayout = TabbedLayout.create();

      VerticalLayout fold = VerticalLayout.create();
      fold.add(Label.create("Some label"));
      fold.add(Button.create("Some Button", () -> Alerts.okError("Clicked!").show()));

      FoldoutLayout layout = FoldoutLayout.create(LocalizeValue.of("Show Me"), fold);
      layout.addStateListener(state -> Alerts.okInfo("State " + state).show());

      tabbedLayout.addTab("FoldoutLayout", layout);

      TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
      splitLayout.setFirstComponent(DockLayout.create().center(Button.create("Left")));
      splitLayout.setSecondComponent(DockLayout.create().center(Button.create("Second")));

      tabbedLayout.addTab("SplitLayout", splitLayout);
      return tabbedLayout;
    }

    @RequiredUIAccess
    private Component components() {
      VerticalLayout layout = VerticalLayout.create();

      FileChooserTextBoxBuilder builder = FileChooserTextBoxBuilder.create(null);
      layout.add(builder.build());

      return layout;
    }

    @Nullable
    @Override
    protected Size getDefaultSize() {
      return new Size(500, 500);
    }
  }

  @RequiredUIAccess
  public static void show() {
    new MyWindowWrapper().showAsync();
  }
}
