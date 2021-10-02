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

import consulo.awt.TargetAWT;
import consulo.ui.image.Image;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import java.awt.*;

// from kotlin
public class MenuArrowIcon implements Icon, UIResource {
  private final Icon myIcon;
  private final Icon mySelectedIcon;
  private final Icon myDisabledIcon;

  public MenuArrowIcon(Image icon, Image selectedIcon, Image disabledIcon) {
    myIcon = TargetAWT.to(icon);
    mySelectedIcon = TargetAWT.to(selectedIcon);
    myDisabledIcon = TargetAWT.to(disabledIcon);
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    if (c instanceof JMenuItem mi) {
      if (!mi.getModel().isEnabled()) {
        myDisabledIcon.paintIcon(c, g, x, y);
      }
      else if (mi.getModel().isArmed() || (mi instanceof JMenu && mi.getModel().isSelected())) {
        mySelectedIcon.paintIcon(c, g, x, y);
      }
      else {
        myIcon.paintIcon(c, g, x, y);
      }
    }
  }

  @Override
  public int getIconWidth() {
    return myIcon.getIconWidth();
  }

  @Override
  public int getIconHeight() {
    return myIcon.getIconHeight();
  }
}
