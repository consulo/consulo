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
package consulo.desktop.swt.ui.impl;

import consulo.ui.MenuBar;
import consulo.ui.MenuItem;
import consulo.ui.annotation.RequiredUIAccess;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 12/12/2021
 */
public class DesktopSwtMenuBar extends SWTComponentDelegate<Menu> implements MenuBar {
  @Override
  protected Menu createSWT(Composite parent) {
    return new Menu((Shell)parent, SWT.MENU);
  }

  @Override
  public void clear() {

  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public MenuBar add(@Nonnull MenuItem menuItem) {
    return this;
  }
}
