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
package consulo.ui.desktop.internal.taskBar;

import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.ui.Window;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author VISTALL
 * @since 2020-10-06
 */
public class Windows7TaskBarImpl extends DefaultJava9TaskBarImpl {
  public Windows7TaskBarImpl() {
  }

  private static Color errorBadgeShadowColor = new Color(0, 0, 0, 102);
  private static Color errorBadgeMainColor = new Color(255, 98, 89);
  private static Color errorBadgeTextBackgroundColor = new Color(0, 0, 0, 39);

  @Override
  protected void setTextBadgeUnsupported(@Nonnull Window window, String text) {
    if (myTaskbar.isSupported(Taskbar.Feature.ICON_BADGE_IMAGE_WINDOW)) {
      BufferedImage image = null;
      if (text != null) {
        int size = 16;
        image = UIUtil.createImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        int shadowRadius = 14;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(errorBadgeShadowColor);
        g.fillRoundRect(size / 2 - shadowRadius / 2, size / 2 - shadowRadius / 2, shadowRadius, shadowRadius, size, size);

        int mainRadius = 12;
        g.setPaint(errorBadgeMainColor);
        g.fillRoundRect(size / 2 - mainRadius / 2, size / 2 - mainRadius / 2, mainRadius, mainRadius, size, size);

        Font font = g.getFont();
        g.setFont(new Font(font.getName(), Font.BOLD, 9));
        FontMetrics fontMetrics = g.getFontMetrics();

        int textWidth = fontMetrics.stringWidth(text);
        int textHeight = UIUtil.getHighestGlyphHeight(text, font, g);

        g.setPaint(errorBadgeTextBackgroundColor);
        g.fillOval(size / 2 - textWidth / 2, size / 2 - textHeight / 2, textWidth, textHeight);

        g.setColor(Color.white);
        g.drawString(text, size / 2 - textWidth / 2, size / 2 - fontMetrics.getHeight() / 2 + fontMetrics.getAscent());
      }

      myTaskbar.setWindowIconBadge(TargetAWT.to(window), image);
    }
  }
}
