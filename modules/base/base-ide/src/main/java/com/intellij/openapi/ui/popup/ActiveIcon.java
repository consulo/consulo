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

package com.intellij.openapi.ui.popup;

import consulo.annotation.DeprecationInfo;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

@Deprecated
@DeprecationInfo("Use Image#stated")
public class ActiveIcon implements Icon {

  private boolean myActive = true;

  private consulo.ui.image.Image myRegular;
  private consulo.ui.image.Image myInactive;

  public ActiveIcon(consulo.ui.image.Image icon) {
    this(icon, icon);
  }

  public ActiveIcon(@Nullable final consulo.ui.image.Image regular, @Nullable final consulo.ui.image.Image inactive) {
    setIcons(regular, inactive);
  }

  protected void setIcons(@Nullable final consulo.ui.image.Image regular, @Nullable final consulo.ui.image.Image inactive) {
    myRegular = regular != null ? regular : Image.empty(0);
    myInactive = inactive != null ? inactive : myRegular;
  }

  public consulo.ui.image.Image getRegular() {
    return myRegular;
  }

  public consulo.ui.image.Image getInactive() {
    return myInactive;
  }

  private consulo.ui.image.Image getIcon() {
    return myActive ? getRegular() : getInactive();
  }

  public void setActive(final boolean active) {
    myActive = active;
  }

  @Override
  public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
    Icon icon = TargetAWT.to(getIcon());
    
    icon.paintIcon(c, g, x, y);
  }

  @Override
  public int getIconWidth() {
    return getIcon().getWidth();
  }

  @Override
  public int getIconHeight() {
    return getIcon().getHeight();
  }
}
