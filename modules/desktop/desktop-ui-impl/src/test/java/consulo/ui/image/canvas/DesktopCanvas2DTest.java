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
package consulo.ui.image.canvas;

import com.mxgraph.util.mxUtils;
import consulo.ui.internal.image.canvas.DesktopCanvas2DImpl;
import consulo.ui.style.StandardColors;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author VISTALL
 * @since 2018-06-15
 */
public class DesktopCanvas2DTest {
  public static void main(String[] args) throws Exception {
    BufferedImage bufferedImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);

    Graphics2D graphics = bufferedImage.createGraphics();
    mxUtils.setAntiAlias(graphics, true, true);
    DesktopCanvas2DImpl ctx = new DesktopCanvas2DImpl(graphics);

    ctx.setStrokeColor(StandardColors.RED.getStaticValue());
    Canvas2DHelper.roundRectangle(ctx, 0, 0, 16, 16, 2, true, true);
    ctx.stroke();


    ImageIO.write(bufferedImage, "png", new File("W:\\_l2\\test.png"));
  }
}
