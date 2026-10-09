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

import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
final class DesktopAWTHtmlImage extends java.awt.Image {
    private final Image myImage;

    private @Nullable Image myResized;

    private @Nullable BufferedImage myRaster;

    DesktopAWTHtmlImage(Image image) {
        myImage = image;
    }

    void paint(Graphics g, int x, int y, int width, int height) {
        Icon icon = TargetAWT.to(resize(width, height));
        icon.paintIcon(null, g, x, y);
    }

    private Image resize(int width, int height) {
        if (myImage.getWidth() == width && myImage.getHeight() == height) {
            return myImage;
        }

        Image resized = myResized;
        if (resized == null || resized.getWidth() != width || resized.getHeight() != height) {
            resized = ImageEffects.resize(myImage, width, height);
            myResized = resized;
        }
        return resized;
    }

    @Override
    public int getWidth(@Nullable ImageObserver observer) {
        return myImage.getWidth();
    }

    @Override
    public int getHeight(@Nullable ImageObserver observer) {
        return myImage.getHeight();
    }

    @Override
    @SuppressWarnings("UndesirableClassUsage")
    public ImageProducer getSource() {
        BufferedImage raster = myRaster;
        if (raster == null) {
            raster = new BufferedImage(Math.max(1, myImage.getWidth()), Math.max(1, myImage.getHeight()), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = raster.createGraphics();
            try {
                paint(graphics, 0, 0, myImage.getWidth(), myImage.getHeight());
            }
            finally {
                graphics.dispose();
            }
            myRaster = raster;
        }
        return raster.getSource();
    }

    @Override
    public Graphics getGraphics() {
        throw new UnsupportedOperationException("not an offscreen image");
    }

    @Override
    public Object getProperty(String name, @Nullable ImageObserver observer) {
        return UndefinedProperty;
    }
}
