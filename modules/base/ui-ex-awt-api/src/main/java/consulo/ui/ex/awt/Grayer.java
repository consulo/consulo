/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.ui.style.StyleManager;

import jakarta.annotation.Nonnull;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public final class Grayer extends Graphics2DDelegate {
  private final Color myBackground;

  public Grayer(Graphics2D g2d, Color background) {
    super(g2d);
    myBackground = background;
  }

  @Override
  public void setColor(Color color) {
    if (color != null && !myBackground.equals(color)) {
      //noinspection UseJBColor
      color = new Color(UIUtil.getGrayFilter(StyleManager.get().getCurrentStyle().isDark()).filterRGB(0, 0, color.getRGB()));
    }
    super.setColor(color);
  }

  @Nonnull
  @Override
  public Graphics create() {
    return new Grayer((Graphics2D)super.create(), myBackground);
  }
}
