package consulo.ui.desktop.internal.image.libraryImage;

import com.intellij.ui.paint.PaintUtil;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.JBHiDPIScaledImage;
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

  private java.awt.Image myCachedImage;

  public DesktopSvgImageImpl(@Nonnull SVGDiagram x1Diagram, @Nullable SVGDiagram x2Diagram, int width, int height, @Nullable Supplier<ImageFilter> imageFilterSupplier) {
    super(width, height, imageFilterSupplier);
    myX1Diagram = x1Diagram;
    myX2Diagram = x2Diagram;
  }

  @Nonnull
  @Override
  protected DesktopSvgImageImpl withFilter(@Nullable Supplier<ImageFilter> filter) {
    return new DesktopSvgImageImpl(myX1Diagram, myX2Diagram, myWidth, myHeight, filter);
  }

  @SuppressWarnings("UndesirableClassUsage")
  @Nonnull
  protected java.awt.Image calcImage(@Nonnull Graphics originalGraphics) {
    float width = myWidth;
    float height = myHeight;

    SVGDiagram targetDiagram = myX1Diagram;
    float scale = 1f;
    if ((scale = JBUIScale.sysScale((Graphics2D)originalGraphics)) > 1f) {
      targetDiagram = myX2Diagram != null ? myX2Diagram : targetDiagram;
      
      width *= scale;
      height *= scale;
    }

    JBHiDPIScaledImage image = new JBHiDPIScaledImage(scale, width, height, BufferedImage.TYPE_INT_ARGB, PaintUtil.RoundingMode.ROUND);
    Graphics2D g = image.createGraphics();
    GraphicsUtil.setupAAPainting(g);
    paintIcon(targetDiagram, g, 0, 0, width, height);
    g.dispose();

    image = image.scale(JBUI.scale(1f));

    java.awt.Image toPaintImage = image;
    if (myFilter != null) {
      ImageFilter imageFilter = myFilter.get();
      toPaintImage = ImageUtil.filter(toPaintImage, imageFilter);
    }

    toPaintImage = ImageUtil.ensureHiDPI(toPaintImage, JBUI.ScaleContext.create((Graphics2D)originalGraphics));
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
