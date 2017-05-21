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
package consulo.ui.internal;

import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.SimpleTextAttributes;
import consulo.ui.ImageRef;
import consulo.ui.ListItemPresentation;
import consulo.ui.TextStyle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 12-Jun-16
 * <p/>
 * some dummy impl
 */
public class DesktopListItemPresentationImpl<E> implements ListItemPresentation {
  private ColoredListCellRenderer<E> myRenderer;

  public DesktopListItemPresentationImpl(ColoredListCellRenderer<E> renderer) {
    myRenderer = renderer;
  }

  @Override
  public void append(@NotNull ImageRef... imageRefs) {
    if (imageRefs.length == 0) {
      throw new IllegalArgumentException();
    }

    Icon icon = null;
    if (imageRefs.length == 1) {
      icon = ((DesktopImageRefImpl)imageRefs[0]).getIcon();
    }
    else {
      LayeredIcon layeredIcon = new LayeredIcon(imageRefs.length);
      icon = layeredIcon;
      for (int i = 0; i < imageRefs.length; i++) {
        ImageRef imageRef = imageRefs[i];
        layeredIcon.setIcon(((DesktopImageRefImpl)imageRef).getIcon(), i);
      }
    }

    myRenderer.setIcon(icon);
  }

  @Override
  public void append(@NotNull String text) {
    myRenderer.append(text);
  }

  @Override
  public void append(@NotNull String text, @NotNull TextStyle... styles) {
    if (styles[0] == TextStyle.BOLD) {
      myRenderer.append(text, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }
    else {
      append(text);
    }
  }
}
