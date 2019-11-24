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
package consulo.ui.app;

import com.intellij.openapi.util.Disposer;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.Layout;
import consulo.ui.shared.Size;
import consulo.ui.shared.border.BorderPosition;
import consulo.ui.shared.border.BorderStyle;
import consulo.ui.style.ComponentColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 25-Oct-17
 * <p>
 * Wrapper like {@link com.intellij.openapi.ui.DialogWrapper} but for Consulo UI
 */
public abstract class WindowWrapper {
  private Window myWindow;
  private String myTitle;

  public WindowWrapper(@Nonnull String title) {
    myTitle = title;
  }

  @Nonnull
  @RequiredUIAccess
  protected abstract Component createCenterComponent();

  @Nullable
  protected Size getDefaultSize() {
    return null;
  }

  /**
   * Not block UI
   */
  @RequiredUIAccess
  public void showAsync() {
    if (myWindow != null) {
      throw new IllegalArgumentException();
    }

    myWindow = Window.createModal(myTitle);
    myWindow.setClosable(false);
    Size defaultSize = getDefaultSize();
    if (defaultSize != null) {
      myWindow.setSize(defaultSize);
    }

    Layout rootLayout = buildRootLayout();
    myWindow.setContent(rootLayout);

    myWindow.show();
  }

  @Nonnull
  @RequiredUIAccess
  protected Layout buildRootLayout() {
    DockLayout rootLayout = DockLayout.create();
    rootLayout.center(createCenterComponent());
    rootLayout.bottom(buildButtonsLayout());
    return rootLayout;
  }

  @Nonnull
  @RequiredUIAccess
  protected Layout buildButtonsLayout() {
    DockLayout dockLayout = DockLayout.create();
    dockLayout.addBorder(BorderPosition.TOP, BorderStyle.LINE, ComponentColors.BORDER, 1);

    HorizontalLayout bottomLayout = HorizontalLayout.create();
    Button okButton = Button.create("OK", () -> close());
    bottomLayout.add(okButton);
    Button cancelButton = Button.create("Cancel", () -> close());
    bottomLayout.add(cancelButton);

    bottomLayout.addBorders(BorderStyle.EMPTY, null, 5);

    return dockLayout.right(bottomLayout);
  }

  @RequiredUIAccess
  public void close() {
    if (myWindow == null) {
      return;
    }
    myWindow.close();
    Disposer.dispose(myWindow);
    myWindow = null;
  }
}
