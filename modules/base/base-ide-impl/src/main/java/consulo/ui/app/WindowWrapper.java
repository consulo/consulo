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

import consulo.disposer.Disposer;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.Layout;
import consulo.ui.Size;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
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

  private Boolean myPreOkEnabled;

  private Button myOkButton;

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

    myWindow = Window.create(myTitle, WindowOptions.builder().owner(Window.getActiveWindow()).build());
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
    myOkButton = Button.create("OK", e -> doOKAction());
    if(myPreOkEnabled != null) {
      myOkButton.setEnabled(myPreOkEnabled);
    }
    bottomLayout.add(myOkButton);
    Button cancelButton = Button.create("Cancel", e -> doCancelAction());
    bottomLayout.add(cancelButton);

    bottomLayout.addBorders(BorderStyle.EMPTY, null, 5);

    return dockLayout.right(bottomLayout);
  }

  @RequiredUIAccess
  public void doOKAction() {
    close();
  }

  @RequiredUIAccess
  public void setOKEnabled(boolean value) {
    if(myOkButton == null) {
      myPreOkEnabled = value;
    }
    else {
      myOkButton.setEnabled(value);
    }
  }

  @RequiredUIAccess
  public void doCancelAction() {
    close();
  }

  @RequiredUIAccess
  public void close() {
    if (myWindow == null) {
      return;
    }
    myWindow.close();
    Disposer.dispose(myWindow);
    myWindow = null;
    myOkButton = null;
  }
}
