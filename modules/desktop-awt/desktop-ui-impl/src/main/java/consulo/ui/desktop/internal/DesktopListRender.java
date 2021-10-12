/*
 * Copyright 2013-2020 consulo.io
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

import com.intellij.ide.ui.DesktopAntialiasingTypeUtil;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.UIUtil;
import consulo.desktop.util.awt.DesktopAntialiasingType;
import consulo.ui.AntialiasingType;
import consulo.ui.TextItemPresentation;
import consulo.ui.TextItemRender;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2020-08-23
 */
class DesktopListRender<E> extends ColoredListCellRenderer<E> {
  private Supplier<AntialiasingType> myAntialiasingType = () -> DesktopAntialiasingTypeUtil.getAntialiasingTypeForSwingComponent().to();
  private Supplier<TextItemRender<E>> myRenderSupplier;

  public DesktopListRender(Supplier<TextItemRender<E>> renderSupplier) {
    myRenderSupplier = renderSupplier;
  }

  @Override
  protected void customizeCellRenderer(@Nonnull JList<? extends E> list, E value, int index, boolean selected, boolean hasFocus) {
    DesktopTextItemPresentationImpl render = new DesktopTextItemPresentationImpl(this) {
      @Nonnull
      @Override
      public TextItemPresentation withAntialiasingType(@Nonnull AntialiasingType type) {
        myAntialiasingType = () -> type;
        updateUI();
        return super.withAntialiasingType(type);
      }
    };
    myRenderSupplier.get().render(render, index, value);
  }

  @Override
  protected void applyAdditionalHints(@Nonnull Graphics2D g2d) {
    super.applyAdditionalHints(g2d);

    if(myAntialiasingType == null) {
      return;
    }

    AntialiasingType aa = myAntialiasingType.get();
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, UIUtil.getLcdContrastValue());
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, DesktopAntialiasingType.from(aa).getHint());
  }

  @Override
  public void updateUI() {
    if(myAntialiasingType == null) {
      return;
    }
    GraphicsUtil.setAntialiasingType(this, myAntialiasingType.get());
  }
}
