/*
 * Copyright 2013-2016 consulo.io
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
package consulo.desktop.awt.ui.impl;

import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
class DesktopMenuImpl extends SwingComponentDelegate<JMenu> implements Menu {
  class MyJMenu extends JMenu implements FromSwingComponentWrapper {
    MyJMenu(String s) {
      super(s);
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopMenuImpl.this;
    }
  }

  public DesktopMenuImpl(String text) {
    myComponent = new MyJMenu(text);
  }

  @Override
  public void setIcon(@Nullable Image icon) {
    myComponent.setIcon(TargetAWT.to(icon));
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Menu add(@Nonnull MenuItem menuItem) {
    if (menuItem instanceof MenuSeparator) {
      myComponent.addSeparator();
      return this;
    }
    myComponent.add((JMenuItem)TargetAWT.to(menuItem));
    return this;
  }

  @Nonnull
  @Override
  public String getText() {
    return myComponent.getText();
  }
}
