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
package consulo.ui.internal.image.canvas;

import com.mxgraph.util.mxUtils;
import consulo.ui.TextAttribute;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.image.canvas.Canvas2DFont;
import consulo.ui.style.StandardColors;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author VISTALL
 * @since 2018-06-21
 */
public class Main {
  public static void main(String[] args) throws Exception {
    int width = 100;
    int height = 100;
    BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    Graphics2D graphics = bufferedImage.createGraphics();
    mxUtils.setAntiAlias(graphics, true, true);
    DesktopCanvas2DImpl ctx = new DesktopCanvas2DImpl(graphics);

    ctx.setFillStyle(StandardColors.BLACK);
    ctx.fillRect(0, 0, width, height);

    ctx.setFillStyle(StandardColors.WHITE);
    ctx.setFont(new Canvas2DFont("Arial", (int)(width / 1.7f) , TextAttribute.STYLE_BOLD));

    ctx.setTextAlign(Canvas2D.TextAlign.center);
    ctx.setTextBaseline(Canvas2D.TextBaseline.middle);

    ctx.fillText("9", width / 2, height / 2);



    ImageIO.write(bufferedImage, "png", new File("W:\\_l2\\test.png"));
  }
}
