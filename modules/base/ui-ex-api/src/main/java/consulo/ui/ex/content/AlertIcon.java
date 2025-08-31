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
package consulo.ui.ex.content;

import consulo.ui.image.Image;

import javax.swing.*;
import java.awt.*;

// TODO [VISTALL] we need impl UI Image, due we don't have unified impl
public class AlertIcon implements Icon, Image {

  private Icon myIcon;
  private int myVShift;
  private int myHShift;

  public AlertIcon(Icon icon) {
    this(icon, 0, 0);
  }

  public AlertIcon(Icon icon, int VShift, int HShift) {
    myIcon = icon;
    myVShift = VShift;
    myHShift = HShift;
  }

  public Icon getIcon() {
    return myIcon;
  }

  public int getVShift() {
    return myVShift;
  }

  public int getHShift() {
    return myHShift;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    myIcon.paintIcon(c, g, x + myHShift, y + myVShift);
  }

  @Override
  public int getIconWidth() {
    return myIcon.getIconWidth();
  }

  @Override
  public int getIconHeight() {
    return myIcon.getIconHeight();
  }

  @Override
  public int getHeight() {
    return myIcon.getIconHeight();
  }

  @Override
  public int getWidth() {
    return myIcon.getIconWidth();
  }
}
