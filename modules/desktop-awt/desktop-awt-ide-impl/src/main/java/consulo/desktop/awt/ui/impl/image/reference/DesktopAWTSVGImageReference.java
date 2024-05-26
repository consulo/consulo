/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.ui.impl.image.reference;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import consulo.ui.ex.awt.ImageUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.internal.RetinaImage;
import consulo.ui.ex.awt.util.GraphicsUtil;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author VISTALL
 * @since 26.05.2024
 */
public class DesktopAWTSVGImageReference extends DesktopAWTImageReference {
  private final SVGDocument myX1Diagram;
  private final SVGDocument myX2Diagram;

  public DesktopAWTSVGImageReference(SVGDocument x1Diagram, SVGDocument x2Diagram) {
    myX1Diagram = x1Diagram;
    myX2Diagram = x2Diagram;
  }

  @Override
  public void draw(@Nonnull JBUI.ScaleContext ctx,
                   @Nonnull Graphics2D graphics,
                   int x,
                   int y,
                   int width,
                   int height) {
    GraphicsUtil.setupAntialiasing(graphics);

    double sysScale = ctx.getScale(JBUI.ScaleType.SYS_SCALE);
    double userScale = ctx.getScale(JBUI.ScaleType.USR_SCALE);
    SVGDocument target;
    if ((sysScale > 1 || userScale > 1) && myX2Diagram != null) {
      target = myX2Diagram;
    }
    else {
      target = myX1Diagram;
    }

    Image image = ImageUtil.createImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = ((BufferedImage)image).createGraphics();
    GraphicsUtil.setupAntialiasing(g);
    target.render(null, g, new ViewBox(0, 0, width, height));
    g.dispose();

    if (userScale > 1f) {
      image = RetinaImage.createFrom(image, userScale, null);
    }

    UIUtil.drawImage(graphics, image, new Rectangle(x, y, width, height), null);
  }
}
