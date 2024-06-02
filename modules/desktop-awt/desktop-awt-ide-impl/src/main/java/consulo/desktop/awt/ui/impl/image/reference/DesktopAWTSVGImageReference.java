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
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.util.GraphicsUtil;
import jakarta.annotation.Nonnull;

import java.awt.*;

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
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    SVGDocument target = myX1Diagram;
    if (myX2Diagram != null) {
      double sysScale = ctx.getScale(JBUI.ScaleType.SYS_SCALE);
      double userScale = ctx.getScale(JBUI.ScaleType.USR_SCALE);
      if (sysScale > 1 || userScale > 1) {
        target = myX2Diagram;
      }
    }

    target.render(null, graphics, new ViewBox(x, y, width, height));
  }
}
