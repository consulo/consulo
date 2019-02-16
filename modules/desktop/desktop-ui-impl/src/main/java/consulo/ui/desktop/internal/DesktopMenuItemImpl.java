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

import consulo.ui.MenuItem;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
class DesktopMenuItemImpl extends SwingComponentDelegate<JMenuItem> implements MenuItem {
  public DesktopMenuItemImpl(String text) {
    myComponent = new JMenuItem(text);
  }

  @Nonnull
  @Override
  public String getText() {
    return myComponent.getText();
  }
}
