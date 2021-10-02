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
package com.intellij.ide.ui.laf;

import com.intellij.openapi.util.NotNullLazyValue;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.util.function.Supplier;

// from kotlin

//TODO - dont call it inside laf initialize
public class MenuArrowIcon implements Icon, UIResource {
  private final NotNullLazyValue<Icon> myIcon;
  private final NotNullLazyValue<Icon> mySelectedIcon;
  private final NotNullLazyValue<Icon> myDisabledIcon;

  public MenuArrowIcon(Supplier<Image> icon, Supplier<Image> selectedIcon, Supplier<Image> disabledIcon) {
    myIcon = NotNullLazyValue.createValue(() -> TargetAWT.to(icon.get()));
    mySelectedIcon = NotNullLazyValue.createValue(() -> TargetAWT.to(selectedIcon.get()));
    myDisabledIcon = NotNullLazyValue.createValue(() -> TargetAWT.to(disabledIcon.get()));
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    if (c instanceof JMenuItem mi) {
      if (!mi.getModel().isEnabled()) {
        myDisabledIcon.get().paintIcon(c, g, x, y);
      }
      else if (mi.getModel().isArmed() || (mi instanceof JMenu && mi.getModel().isSelected())) {
        mySelectedIcon.get().paintIcon(c, g, x, y);
      }
      else {
        myIcon.get().paintIcon(c, g, x, y);
      }
    }
  }

  @Override
  public int getIconWidth() {
    return myIcon.get().getIconWidth();
  }

  @Override
  public int getIconHeight() {
    return myIcon.get().getIconHeight();
  }
}
