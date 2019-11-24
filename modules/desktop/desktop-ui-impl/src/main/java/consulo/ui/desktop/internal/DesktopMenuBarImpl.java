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
package consulo.ui.desktop.internal;

import consulo.awt.TargetAWT;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class DesktopMenuBarImpl extends SwingComponentDelegate<JMenuBar> implements MenuBar {
  class MyJMenu extends JMenuBar implements FromSwingComponentWrapper {

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopMenuBarImpl.this;
    }
  }

  public DesktopMenuBarImpl() {
    myComponent = new MyJMenu();
  }

  @Override
  public void clear() {
    myComponent.removeAll();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public MenuBar add(@Nonnull MenuItem menuItem) {
    if (menuItem instanceof Menu) {
      myComponent.add((JMenu)TargetAWT.to(menuItem));
    }
    else {
      DesktopMenuImpl menu = new DesktopMenuImpl(menuItem.getText());
      myComponent.add((JMenu)TargetAWT.to(menu));
    }
    return this;
  }
}
