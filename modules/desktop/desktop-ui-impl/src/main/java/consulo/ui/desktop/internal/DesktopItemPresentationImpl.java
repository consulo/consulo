/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.desktop.internal;

import com.intellij.ui.ColoredListCellRenderer;
import consulo.awt.TargetAWT;
import consulo.awt.impl.TargetAWTFacadeImpl;
import consulo.localize.LocalizeValue;
import consulo.ui.ItemPresentation;
import consulo.ui.TextAttribute;
import consulo.ui.font.Font;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
class DesktopItemPresentationImpl<E> implements ItemPresentation {
  private ColoredListCellRenderer<E> myRenderer;

  public DesktopItemPresentationImpl(ColoredListCellRenderer<E> renderer) {
    myRenderer = renderer;
  }

  @Override
  public void clearText() {
    Icon icon = myRenderer.getIcon();
    myRenderer.clear();
    myRenderer.setIcon(icon);
  }

  @Nonnull
  @Override
  public ItemPresentation withFont(@Nonnull Font font) {
    myRenderer.setFont(TargetAWT.to(font));
    return this;
  }

  @Nonnull
  @Override
  public ItemPresentation withIcon(@Nullable Image icon) {
    myRenderer.setIcon(icon);
    return this;
  }

  @Override
  public void append(@Nonnull LocalizeValue text, @Nonnull TextAttribute textAttribute) {
    myRenderer.append(text.get(), TargetAWTFacadeImpl.from(textAttribute));
  }
}
