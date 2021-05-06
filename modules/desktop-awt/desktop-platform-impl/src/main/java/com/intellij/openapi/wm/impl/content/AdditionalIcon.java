/*
 * Copyright 2013-2018 consulo.io
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

import com.intellij.openapi.ui.popup.ActiveIcon;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

/**
 * from kotlin
 */
public abstract class AdditionalIcon {
  private final ActiveIcon myIcon;
  private int myX;

  public AdditionalIcon(@Nonnull ActiveIcon icon) {
    myIcon = icon;
  }

  public Point getCenterPoint() {
    return new Point(getX() + (getIconWidth() / 2), getIconY());
  }

  public int getIconY() {
    Rectangle rectangle = getRectangle();
    return rectangle.y + rectangle.height / 2 - getIconHeight() / 2 + 1;
  }

  public boolean contains(Point point) {
    return getRectangle().contains(point);
  }

  public int getIconWidth() {
    return myIcon.getIconWidth();
  }

  public int getIconHeight() {
    return myIcon.getIconHeight();
  }

  public void paintIcon(Component c, Graphics g) {
    myIcon.setActive(getActive());

    myIcon.paintIcon(c, g, getX(), getIconY());
  }

  public void setX(int x) {
    myX = x;
  }

  public int getX() {
    return myX;
  }

  public abstract Rectangle getRectangle();

  public abstract boolean getActive();

  public abstract boolean getAvailable();

  public abstract Runnable getAction();

  public boolean getAfterText() {
    return true;
  }

  @Nullable
  public String getTooltip() {
    return null;
  }
}
