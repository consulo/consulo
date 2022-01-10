package consulo.ui.desktop.internal.image.libraryImage;

import com.intellij.ui.JBColor;
import com.intellij.util.JBHiDPIScaledImage;
import com.intellij.util.RetinaImage;
import com.intellij.util.io.UnsyncByteArrayInputStream;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageFilter;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2020-09-30
 */
public class DesktopAWTImageImpl extends DesktopInnerImageImpl<DesktopAWTImageImpl> {
  private static final Logger LOG = Logger.getInstance(DesktopAWTImageImpl.class);

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

  private final ImageBytes myX1Data;
  private final ImageBytes myX2Data;

  public DesktopAWTImageImpl(@Nonnull ImageBytes x1Data, @Nullable ImageBytes x2Data, int width, int height, @Nullable Supplier<ImageFilter> imageFilterSupplier) {
    this(x1Data, x2Data, width, height, 1f, imageFilterSupplier);
  }

  public DesktopAWTImageImpl(@Nonnull ImageBytes x1Data, @Nullable ImageBytes x2Data, int width, int height, float scale, @Nullable Supplier<ImageFilter> imageFilterSupplier) {
    super(width, height, scale, imageFilterSupplier);

    myX1Data = x1Data;
    myX2Data = x2Data;
  }

  @Nonnull
  @Override
  protected DesktopAWTImageImpl withFilter(@Nullable Supplier<ImageFilter> filter) {
    return new DesktopAWTImageImpl(myX1Data, myX2Data, myWidth, myHeight, filter);
  }

  @Nonnull
  @Override
  protected DesktopAWTImageImpl withScale(float scale) {
    return new DesktopAWTImageImpl(myX1Data, myX2Data, myWidth, myHeight, scale, myFilter);
  }

  @SuppressWarnings("UndesirableClassUsage")
  @Nonnull
  @Override
  protected Image calcImage(JBUI.ScaleContext ctx) {
    if (ctx == null) {
      ctx = JBUI.ScaleContext.create();
    }

    ImageBytes target = myX1Data;
    double scale = ctx.getScale(JBUI.ScaleType.SYS_SCALE);
    if (scale > 1f || myScale > 1.5f) {
      target = myX2Data != null ? myX2Data : target;
    }

    BufferedImage bufferedImage = target.getOrLoad();
    if (bufferedImage == null) {
      BufferedImage blueImage = new BufferedImage(myWidth, myHeight, BufferedImage.TYPE_INT_ARGB);
      Graphics2D graphics = blueImage.createGraphics();
      graphics.setColor(JBColor.BLUE);
      graphics.fillRect(0, 0, myWidth, myHeight);
      graphics.dispose();
      return blueImage;
    }

    JBHiDPIScaledImage image = new JBHiDPIScaledImage(bufferedImage, myWidth, myHeight, BufferedImage.TYPE_INT_ARGB);

    image = image.scale(getWidth(), getHeight());

    float ideScale = JBUI.scale(1f);

    java.awt.Image toPaintImage = image;
    if (myFilter != null) {
      ImageFilter imageFilter = myFilter.get();

      toPaintImage = ImageUtil.filter(toPaintImage, imageFilter);

      if (ideScale > 1f) {
        toPaintImage = RetinaImage.createFrom(toPaintImage, scale, null);
      }

      toPaintImage = ImageUtil.ensureHiDPI(toPaintImage, ctx);

      if (toPaintImage instanceof JBHiDPIScaledImage) {
        toPaintImage = ((JBHiDPIScaledImage)toPaintImage).scale(getWidth(), getHeight());
      }
    }
    return toPaintImage;
  }
}
