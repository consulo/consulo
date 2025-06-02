/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.desktop.awt.ui.popup;

import consulo.ui.ex.Gray;
import consulo.ui.ex.awt.ImageUtil;
import org.jdesktop.swingx.graphics.ShadowRenderer;
import org.jdesktop.swingx.util.GraphicsUtilities;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author spleaner
 * @author Konstantin Bulenkov
 */
public class ShadowBorderPainter {
    private ShadowBorderPainter() {
    }

    public static Shadow createShadow(Image source, int x, int y, boolean paintSource, int shadowSize) {
        source = ImageUtil.toBufferedImage(source);
        float w = source.getWidth(null);
        float h = source.getHeight(null);
        float ratio = w / h;
        float deltaX = shadowSize;
        float deltaY = shadowSize / ratio;

        Image scaled = source.getScaledInstance((int) (w + deltaX), (int) (h + deltaY), Image.SCALE_FAST);

        BufferedImage s =
            GraphicsUtilities.createCompatibleTranslucentImage(scaled.getWidth(null), scaled.getHeight(null));
        Graphics2D graphics = (Graphics2D) s.getGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(scaled, 0, 0, null);

        BufferedImage shadow = new ShadowRenderer(shadowSize, .2f, Gray.x00).createShadow(s);
        if (paintSource) {
            Graphics imgG = shadow.getGraphics();
            double d = shadowSize * 0.5;
            imgG.drawImage(source, (int) (shadowSize + d), (int) (shadowSize + d / ratio), null);
        }

        return new Shadow(shadow, x - shadowSize - 5, y - shadowSize + 2);
    }

    public static class Shadow {
        int x;
        int y;
        Image image;

        public Shadow(Image image, int x, int y) {
            this.x = x;
            this.y = y;
            this.image = image;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public Image getImage() {
            return image;
        }
    }
}
