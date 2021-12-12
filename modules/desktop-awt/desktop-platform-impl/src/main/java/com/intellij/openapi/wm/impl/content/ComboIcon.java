/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl.content;

import com.intellij.util.ui.JBUI;
import consulo.desktop.ui.laf.idea.darcula.LafIconLookup;

import javax.swing.*;
import java.awt.*;

public abstract class ComboIcon {
  private final Icon myImage;

  public ComboIcon() {
    myImage = LafIconLookup.getIcon("treeExpanded", false, false, true);
  }

  public void paintIcon(final Component c, final Graphics g) {
    Rectangle bounds = c.getBounds();

    // we need it move to center of label text, not label
    int borderTop = JBUI.scale(2);
    myImage.paintIcon(c, g, bounds.x + bounds.width - myImage.getIconWidth(), bounds.y + myImage.getIconHeight() / 2 + borderTop);
  }

  public int getIconWidth() {
    return myImage.getIconWidth();
  }

  public abstract boolean isActive();
}
