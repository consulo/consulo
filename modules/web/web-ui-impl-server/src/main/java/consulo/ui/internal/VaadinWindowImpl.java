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
package consulo.ui.internal;

import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.UIAccess;
import consulo.ui.VaadinUIAccessImpl;
import consulo.ui.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class VaadinWindowImpl extends com.vaadin.ui.Window implements Window, VaadinWrapper {
  public VaadinWindowImpl(boolean modal) {
    setModal(modal);
  }

  @RequiredUIAccess
  @Override
  public void setTitle(@NotNull String title) {
    setCaption(title);
  }

  @RequiredUIAccess
  @Override
  public void setContent(@NotNull Component content) {
    setContent((com.vaadin.ui.Component)content);
  }

  @RequiredUIAccess
  @Override
  public void setMenuBar(@Nullable MenuBar menuBar) {
    throw new UnsupportedOperationException();
  }


  @RequiredUIAccess
  @Override
  public void show() {
    VaadinUIAccessImpl uiAccess = (VaadinUIAccessImpl)UIAccess.get();

    uiAccess.getUI().addWindow(this);
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return null;
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {
    if (size.getWidth() != -1) {
      setWidth(size.getWidth(), Unit.PIXELS);
    }

    if (size.getHeight() != -1) {
      setHeight(size.getHeight(), Unit.PIXELS);
    }
  }
}
