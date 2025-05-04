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
package consulo.ide.impl.ui.app;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.Layout;
import consulo.ui.style.ComponentColors;
import consulo.util.concurrent.AsyncResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 25-Oct-17
 * <p>
 * Wrapper like {@link DialogWrapper} but for Consulo UI
 */
public abstract class WindowWrapper {
  private Window myWindow;
  private String myTitle;

  private Boolean myPreOkEnabled;

  private Button myOkButton;

  private AsyncResult<Void> myResult;

  public WindowWrapper(@Nonnull String title) {
    myTitle = title;
  }

  @Nonnull
  @RequiredUIAccess
  protected abstract Component createCenterComponent(@Nonnull Disposable uiDisposable);

  @Nullable
  protected Size getDefaultSize() {
    return null;
  }

  /**
   * Not block UI
   */
  @RequiredUIAccess
  public AsyncResult<Void> showAsync() {
    if (myWindow != null) {
      throw new IllegalArgumentException();
    }

    myResult = AsyncResult.undefined();

    myWindow = Window.create(myTitle, WindowOptions.builder().owner(Window.getActiveWindow()).build());
    Size defaultSize = getDefaultSize();
    if (defaultSize != null) {
      myWindow.setSize(defaultSize);
    }

    Layout rootLayout = buildRootLayout(myWindow);
    myWindow.setContent(rootLayout);

    myWindow.show();

    return myResult;
  }

  @Nonnull
  @RequiredUIAccess
  protected Layout buildRootLayout(Disposable uiDisposable) {
    DockLayout rootLayout = DockLayout.create();
    rootLayout.center(createCenterComponent(uiDisposable));
    rootLayout.bottom(buildButtonsLayout());
    return rootLayout;
  }

  @Nonnull
  @RequiredUIAccess
  protected Layout buildButtonsLayout() {
    DockLayout dockLayout = DockLayout.create();
    dockLayout.addBorder(BorderPosition.TOP, BorderStyle.LINE, ComponentColors.BORDER, 1);

    HorizontalLayout bottomLayout = HorizontalLayout.create();
    myOkButton = Button.create(CommonLocalize.buttonOk(), e -> doOKAction());
    myOkButton.addStyle(ButtonStyle.PRIMARY);

    if (myPreOkEnabled != null) {
      myOkButton.setEnabled(myPreOkEnabled);
    }
    bottomLayout.add(myOkButton);
    Button cancelButton = Button.create(CommonLocalize.buttonCancel(), e -> doCancelAction());
    bottomLayout.add(cancelButton);

    bottomLayout.addBorders(BorderStyle.EMPTY, null, 5);

    return dockLayout.right(bottomLayout);
  }

  @RequiredUIAccess
  public void doOKAction() {
    close(true);
  }

  @RequiredUIAccess
  public void setOKEnabled(boolean value) {
    if (myOkButton == null) {
      myPreOkEnabled = value;
    }
    else {
      myOkButton.setEnabled(value);
    }
  }

  @RequiredUIAccess
  public void doCancelAction() {
    close(false);
  }

  @RequiredUIAccess
  public void close(boolean isOk) {
    if (myWindow == null) {
      return;
    }
    if (isOk) {
      myResult.setDone();
    }
    else {
      myResult.setRejected();
    }
    myWindow.close();
    Disposer.dispose(myWindow);
    myWindow = null;
    myOkButton = null;
    myResult = null;
  }
}
