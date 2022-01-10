/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui.desktop.internal.image;

import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.desktop.util.awt.UIModificationTracker;
import consulo.ui.desktop.internal.image.canvas.DesktopCanvas2DImpl;
import consulo.ui.image.Image;
import consulo.ui.image.canvas.Canvas2D;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2018-06-15
 */
public class DesktopCanvasImageImpl extends JBUI.RasterJBIcon implements Image {
  private static final UIModificationTracker ourUIModificationTracker = UIModificationTracker.getInstance();

  private final int myWidth;
  private final int myHeight;
  private final Consumer<Canvas2D> myCanvas2DConsumer;

  private long myModificationCount;
  private java.awt.Image myImage;

  public DesktopCanvasImageImpl(int width, int height, Consumer<Canvas2D> canvas2DConsumer) {
    myWidth = width;
    myHeight = height;
    myCanvas2DConsumer = canvas2DConsumer;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    JBUI.ScaleContext ctx = JBUI.ScaleContext.create((Graphics2D)g);

    if (updateScaleContext(ctx) || myModificationCount != ourUIModificationTracker.getModificationCount()) {
      myImage = null;
    }

    if (myImage == null) {
      BufferedImage image = UIUtil.createImage(g, myWidth, myHeight, BufferedImage.TYPE_INT_ARGB);
      Graphics graphics = image.createGraphics();
      GraphicsUtil.setupAAPainting(graphics);

      DesktopCanvas2DImpl canvas2D = new DesktopCanvas2DImpl((Graphics2D)graphics);
      myCanvas2DConsumer.accept(canvas2D);

      myModificationCount = ourUIModificationTracker.getModificationCount();
      myImage = image;
    }

    UIUtil.drawImage(g, myImage, x, y, c);
  }

  @Override
  public int getHeight() {
    return getIconHeight();
  }

  @Override
  public int getWidth() {
    return getIconWidth();
  }

  @Override
  public int getIconWidth() {
    return JBUI.scale(myWidth);
  }

  @Override
  public int getIconHeight() {
    return JBUI.scale(myHeight);
  }
}
