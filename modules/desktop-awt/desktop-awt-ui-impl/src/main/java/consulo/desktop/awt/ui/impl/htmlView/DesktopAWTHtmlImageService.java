/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.ui.impl.htmlView;

import consulo.ui.ex.awt.ImageUtil;
import consulo.ui.ex.awt.UIUtil;
import org.cobraparser.ua.ImageService;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.image.ImageObserver;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
public final class DesktopAWTHtmlImageService extends ImageService {
    @Override
    public int getWidth(Image image, @Nullable ImageObserver observer) {
        int width = ImageUtil.getUserWidth(image);
        return width >= 0 ? width : image.getWidth(observer);
    }

    @Override
    public int getHeight(Image image, @Nullable ImageObserver observer) {
        int height = ImageUtil.getUserHeight(image);
        return height >= 0 ? height : image.getHeight(observer);
    }

    @Override
    public void paint(Graphics g, Image image, int x, int y, int width, int height, @Nullable ImageObserver observer) {
        if (width <= 0 || height <= 0) {
            return;
        }

        if (image instanceof DesktopAWTHtmlImage htmlImage) {
            htmlImage.paint(g, x, y, width, height);
            return;
        }

        Graphics2D graphics = (Graphics2D) g.create();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            UIUtil.drawImage(graphics, image, new Rectangle(x, y, width, height), observer);
        }
        finally {
            graphics.dispose();
        }
    }
}
