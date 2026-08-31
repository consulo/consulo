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
package consulo.desktop.awt.ui.impl.components;

import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * @author Eugene Zhuravlev
 * @since 2003-10-29
 */
public class ScalableIconComponent extends JComponent {
  private final Image myIcon;
  private final Image mySelectedIcon;
  private boolean myIsSelected = false;

  public ScalableIconComponent(Image icon) {
    this(icon, icon);
  }

  public ScalableIconComponent(Image icon, Image selectedIcon) {
    myIcon = icon;
    mySelectedIcon = selectedIcon != null? selectedIcon : icon;
    if (icon != null) {
      Dimension size = new Dimension(icon.getWidth(), icon.getHeight());
      this.setPreferredSize(size);
      this.setMinimumSize(size);
      this.setMaximumSize(size);
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    consulo.ui.image.Image icon = myIsSelected? mySelectedIcon : myIcon;
    if (icon != null) {
      Graphics2D g2 = (Graphics2D)g;

      g2.setBackground(getBackground());
      AffineTransform savedTransform = g2.getTransform();

      double scale = Math.min(1.0, Math.min(((double)getWidth()) / icon.getWidth(), ((double)getHeight()) / icon.getHeight()));

      int width = (int)Math.round(icon.getWidth() * scale);
      int height = (int)Math.round(icon.getHeight() * scale);

      g2.translate((getWidth() - width) / 2, (getHeight() - height) / 2);
      g2.scale(scale, scale);
      TargetAWT.to(icon).paintIcon(this, g2, 0, 0);

      g2.setTransform(savedTransform);
    }

    super.paintComponent(g);
  }

  public final void setSelected(boolean isSelected) {
    myIsSelected = isSelected;
    this.revalidate();
    this.repaint();
  }
}
