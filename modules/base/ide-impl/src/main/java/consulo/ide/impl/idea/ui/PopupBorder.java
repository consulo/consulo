/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;

public interface PopupBorder {

  class Factory {
    private Factory() {
    }

    @Nonnull
    public static Border createEmpty() {
      return JBUI.Borders.empty();
    }

    @Nonnull
    public static Border create(boolean active, boolean windowWithShadow) {
      return new LineBorder(JBColor.border(), 1, UIManager.getInt("Component.arc") > 0);
    }

    public static Border createColored(Color color) {
      return new LineBorder(color, 1, UIManager.getInt("Component.arc") > 0);
    }
  }
}
