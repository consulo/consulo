/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.util.DesktopAntialiasingTypeUtil;
import consulo.ui.ex.awt.FontInfo;
import consulo.ui.ex.SimpleTextAttributes;

import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * @author Sergey.Malenkov
 */
public class FontInfoRenderer extends ColoredListCellRenderer<Object> {
  @Override
  protected void customizeCellRenderer(@Nonnull JList<?> list, Object value, int index, boolean selected, boolean focused) {
    Font font = list.getFont();
    String text = value == null ? "" : value.toString();
    append(text);
    if (value instanceof FontInfo) {
      FontInfo info = (FontInfo)value;
      Integer size = getFontSize();
      Font f = info.getFont(size != null ? size : font.getSize());
      if (f.canDisplayUpTo(text) == -1) {
        setFont(f);
      }
      else {
        append("  Non-latin", SimpleTextAttributes.GRAYED_ATTRIBUTES);
      }
    }
  }

  @Override
  protected void applyAdditionalHints(@Nonnull Graphics2D g) {
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, DesktopAntialiasingTypeUtil.getKeyForCurrentScope(isEditorFont()));
  }

  protected Integer getFontSize() {
    return null;
  }

  protected boolean isEditorFont() {
    return false;
  }
}
