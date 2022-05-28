/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.ex;

import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public  class ColoredStringBuilder implements ColoredTextContainer {
  private final StringBuilder myBuilder = new StringBuilder();
  private Image myIcon;

  public void appendTo(@Nonnull StringBuilder... subBuilders) {
    for (StringBuilder subBuilder : subBuilders) {
      subBuilder.append(myBuilder);
    }
    myBuilder.setLength(0);
  }

  @Override
  public void append(@Nonnull String fragment, @Nonnull SimpleTextAttributes attributes) {
    myBuilder.append(fragment);
  }

  @Override
  public void append(@Nonnull String fragment, @Nonnull SimpleTextAttributes attributes, Object tag) {
    myBuilder.append(fragment);
  }

  @Override
  public void setIcon(@Nullable Image icon) {
    myIcon = icon;
  }

  @Override
  public void setToolTipText(@Nullable String text) {
  }

  public StringBuilder getBuilder() {
    return myBuilder;
  }

  @Nullable
  public Image getIcon() {
    return myIcon;
  }

  @Override
  public String toString() {
    return myBuilder.toString();
  }
}