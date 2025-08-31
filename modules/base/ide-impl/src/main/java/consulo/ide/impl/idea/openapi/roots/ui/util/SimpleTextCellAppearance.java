/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.util;

import consulo.ide.impl.idea.openapi.roots.ui.ModifiableCellAppearanceEx;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

// todo: move to lang-impl ?
public class SimpleTextCellAppearance implements ModifiableCellAppearanceEx {
  private Image myIcon;
  private final SimpleTextAttributes myTextAttributes;
  private final String myText;

  public static SimpleTextCellAppearance regular(@Nonnull String text, @Nullable Image icon) {
    return new SimpleTextCellAppearance(text, icon, SimpleTextAttributes.REGULAR_ATTRIBUTES);
  }

  public static SimpleTextCellAppearance invalid(@Nonnull String text, @Nullable Image icon) {
    return new SimpleTextCellAppearance(text, icon, SimpleTextAttributes.ERROR_ATTRIBUTES);
  }

  public static SimpleTextCellAppearance synthetic(@Nonnull String text, @Nullable Image icon) {
    return new SimpleTextCellAppearance(text, icon, SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
  }

  public SimpleTextCellAppearance(@Nonnull String text,
                                  @Nullable Image icon,
                                  @Nonnull SimpleTextAttributes textAttributes) {
    myIcon = icon;
    myTextAttributes = textAttributes;
    myText = text;
  }

  @Override
  public void customize(@Nonnull ColoredTextContainer component) {
    component.setIcon(myIcon);
    component.append(myText, myTextAttributes);
  }

  @Nullable
  @Override
  public Image getIcon() {
    return myIcon;
  }

  @Override
  @Nonnull
  public String getText() {
    return myText;
  }

  @Nonnull
  public SimpleTextAttributes getTextAttributes() {
    return myTextAttributes;
  }

  @Override
  public void setIcon(@Nullable Image icon) {
    myIcon = icon;
  }
}
