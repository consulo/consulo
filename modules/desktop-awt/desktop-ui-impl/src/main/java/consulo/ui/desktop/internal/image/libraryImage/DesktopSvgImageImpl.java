package consulo.ui.desktop.internal.image.libraryImage;

import com.intellij.ui.paint.PaintUtil;
import com.intellij.util.JBHiDPIScaledImage;
import com.intellij.util.RetinaImage;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageFilter;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2020-09-30
 */
public class DesktopSvgImageImpl extends DesktopInnerImageImpl<DesktopSvgImageImpl> {
  private final SVGDiagram myX1Diagram;
  private final SVGDiagram myX2Diagram;

  public DesktopSvgImageImpl(@Nonnull SVGDiagram x1Diagram, @Nullable SVGDiagram x2Diagram, int width, int height, @Nullable Supplier<ImageFilter> imageFilterSupplier) {
    this(x1Diagram, x2Diagram, width, height, 1f, imageFilterSupplier);
  }

  public DesktopSvgImageImpl(@Nonnull SVGDiagram x1Diagram, @Nullable SVGDiagram x2Diagram, int width, int height, float scale, @Nullable Supplier<ImageFilter> imageFilterSupplier) {
    super(width, height, scale, imageFilterSupplier);
    myX1Diagram = x1Diagram;
    myX2Diagram = x2Diagram;
  }

  @Nonnull
  @Override
  protected DesktopSvgImageImpl withScale(float scale) {
    return new DesktopSvgImageImpl(myX1Diagram, myX2Diagram, myWidth, myHeight, scale, myFilter);
  }

  @Nonnull
  @Override
  protected DesktopSvgImageImpl withFilter(@Nullable Supplier<ImageFilter> filter) {
    return new DesktopSvgImageImpl(myX1Diagram, myX2Diagram, myWidth, myHeight, filter);
  }

  @Override
  @SuppressWarnings("UndesirableClassUsage")
  @Nonnull
  protected java.awt.Image calcImage() {
    float width = myWidth * myScale;
    float height = myHeight * myScale;

    SVGDiagram targetDiagram = myX1Diagram;
    double jvmScale = 1f;
    if ((jvmScale = getScale(JBUI.ScaleType.SYS_SCALE)) > 1f) {
      width *= jvmScale;
      height *= jvmScale;
    }

    float ideScale = JBUI.scale(1f);

    if (jvmScale > 1f || myScale > 1.5f) {
      targetDiagram = myX2Diagram != null ? myX2Diagram : targetDiagram;
    }

    double imageScale = jvmScale;
    // downscale
    if (myScale < 1f) {
      imageScale = 1f;
      width = myWidth;
      height = myHeight;
    }

    JBHiDPIScaledImage image = new JBHiDPIScaledImage(imageScale, width, height, BufferedImage.TYPE_INT_ARGB, PaintUtil.RoundingMode.ROUND);
    Graphics2D g = image.createGraphics();
    GraphicsUtil.setupAAPainting(g);
    paintIcon(targetDiagram, g, 0, 0, width, height);
    g.dispose();

    if (myScale < 1) {
      image = image.scale(ideScale * myScale * jvmScale);
    }
    else {
      image = image.scale(ideScale * myScale);
    }

    java.awt.Image toPaintImage = image;
    if (myFilter != null) {
      ImageFilter imageFilter = myFilter.get();

      toPaintImage = ImageUtil.filter(toPaintImage, imageFilter);

      if (ideScale > 1f) {
        toPaintImage = RetinaImage.createFrom(toPaintImage, jvmScale, null);
      }
    }

    toPaintImage = ImageUtil.ensureHiDPI(toPaintImage, JBUI.ScaleContext.create(JBUI.Scale.create(getScale(JBUI.ScaleType.SYS_SCALE), JBUI.ScaleType.SYS_SCALE)));
    return toPaintImage;
  }

  private void paintIcon(SVGDiagram diagram, Graphics2D g, int x, int y, float width, float height) {
    diagram.setDeviceViewport(new Rectangle((int)width, (int)height));

    g.translate(x, y);
    diagram.setIgnoringClipHeuristic(true);


    AffineTransform oldXform = g.getTransform();
    g.transform(new AffineTransform());

    try {
      diagram.render(g);
    }
    catch (SVGException e) {
      throw new RuntimeException(e);
    }

    g.setTransform(oldXform);

    g.translate(-x, -y);
  }
}
