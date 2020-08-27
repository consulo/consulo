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

import consulo.ui.image.Image;

import javax.annotation.Nullable;

public class IconButton extends ActiveIcon {

  private String myTooltip;

  private Image myHovered;

  public IconButton(final String tooltip, @Nullable final Image regular, @Nullable final Image hovered, @Nullable final Image inactive) {
    super(regular, inactive);
    myTooltip = tooltip;
    setHovered(hovered);
  }

  private void setHovered(final Image hovered) {
    myHovered = hovered != null ? hovered : getRegular();
  }

  public IconButton(final String tooltip, final Image regular, final Image hovered) {
    this(tooltip, regular, hovered, regular);
  }

  public IconButton(final String tooltip, final Image regular) {
    this(tooltip, regular, regular, regular);
  }

  protected void setIcons(@Nullable final Image regular, @Nullable final Image inactive, @Nullable final Image hovered) {
    setIcons(regular, inactive);
    setHovered(hovered);
  }

  public Image getHovered() {
    return myHovered;
  }

  public String getTooltip() {
    return myTooltip;
  }
}
