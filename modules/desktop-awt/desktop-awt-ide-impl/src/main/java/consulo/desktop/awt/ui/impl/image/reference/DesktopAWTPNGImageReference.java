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

import consulo.logging.Logger;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.internal.RetinaImage;
import consulo.util.io.UnsyncByteArrayInputStream;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 26.05.2024
 */
public class DesktopAWTPNGImageReference extends DesktopAWTImageReference {
  private static final Logger LOG = Logger.getInstance(DesktopAWTPNGImageReference.class);

  public static class ImageBytes {
    public static ImageBytes of(@Nullable byte[] data) {
      return data != null ? new ImageBytes(data, null) : null;
    }

    private volatile byte[] myBytes;
    private BufferedImage myImage;

    public ImageBytes(@Nullable byte[] bytes, @Nullable BufferedImage image) {
      myBytes = bytes;
      myImage = image;
    }

    @Nullable
    public BufferedImage getOrLoad() {
      if (myBytes == null && myImage == null) {
        return null;
      }

      if (myImage != null) {
        return myImage;
      }

      byte[] bytes = myBytes;
      if (bytes != null) {
        try {
          BufferedImage image = ImageIO.read(new UnsyncByteArrayInputStream(bytes));
          myImage = image;
          myBytes = null;
          return image;
        }
        catch (IOException e) {
          LOG.warn(e);
        }
      }

      return myImage;
    }
  }

  @Nonnull
  private final ImageBytes myX1Data;
  @Nullable
  private final ImageBytes myX2Data;

  public DesktopAWTPNGImageReference(@Nonnull ImageBytes x1Data, @Nullable ImageBytes x2Data) {
    myX1Data = x1Data;
    myX2Data = x2Data;
  }

  @Override
  public void draw(@Nonnull JBUI.ScaleContext ctx,
                   @Nonnull Graphics2D graphics,
                   int x,
                   int y,
                   int width,
                   int height) {
    ImageBytes target = myX1Data;
    double sysScale = ctx.getScale(JBUI.ScaleType.SYS_SCALE);
    double userScale = ctx.getScale(JBUI.ScaleType.USR_SCALE);

    if ((sysScale > 1f || userScale > 1f) && myX2Data != null) {
      target = myX2Data;
    }

    Image image = target.getOrLoad();
    if (image == null) {
      graphics.setColor(JBColor.BLUE);
      graphics.fillRect(x, y, width, height);
    }
    else {
      if (userScale > 1f) {
        image = RetinaImage.createFrom(image, userScale, null);
      }

      UIUtil.drawImage(graphics, image, new Rectangle(x, y, width, height), null);
    }
  }
}
