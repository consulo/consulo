package consulo.ui.desktop.internal.image.libraryImage;

import com.intellij.ui.paint.PaintUtil;
import com.intellij.util.JBHiDPIScaledImage;
import com.intellij.util.RetinaImage;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import org.imgscalr.Scalr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageFilter;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2020-09-30
 */
public class DesktopSvgImageImpl extends DesktopInnerImageImpl<DesktopSvgImageImpl> {
  private static final String PLATFORM_ICON_GROUP = "consulo.platform.base.PlatformIconGroup";
  private static final Set<String> ICONS = Set.of("icon16", "icon16_sandbox");

  private final SVGDiagram myX1Diagram;
  private final SVGDiagram myX2Diagram;
  private final String myGroupId;
  private final String myImageId;

  public DesktopSvgImageImpl(@Nonnull SVGDiagram x1Diagram,
                             @Nullable SVGDiagram x2Diagram,
                             int width,
                             int height,
                             @Nullable Supplier<ImageFilter> imageFilterSupplier,
                             String groupId,
                             String imageId) {
    this(x1Diagram, x2Diagram, width, height, 1f, imageFilterSupplier, groupId, imageId);
  }

  public DesktopSvgImageImpl(@Nonnull SVGDiagram x1Diagram,
                             @Nullable SVGDiagram x2Diagram,
                             int width,
                             int height,
                             float scale,
                             @Nullable Supplier<ImageFilter> imageFilterSupplier,
                             String groupId,
                             String imageId) {
    super(width, height, scale, imageFilterSupplier);
    myX1Diagram = x1Diagram;
    myX2Diagram = x2Diagram;
    myGroupId = groupId;
    myImageId = imageId;
  }

  @Nonnull
  @Override
  protected DesktopSvgImageImpl withScale(float scale) {
    return new DesktopSvgImageImpl(myX1Diagram, myX2Diagram, myWidth, myHeight, scale, myFilter, myGroupId, myImageId);
  }

  @Nonnull
  @Override
  protected DesktopSvgImageImpl withFilter(@Nullable Supplier<ImageFilter> filter) {
    return new DesktopSvgImageImpl(myX1Diagram, myX2Diagram, myWidth, myHeight, filter, myGroupId, myImageId);
  }

  @Override
  @SuppressWarnings("UndesirableClassUsage")
  @Nonnull
  protected java.awt.Image calcImage(@Nullable JBUI.ScaleContext ctx) {
    if (ctx == null) {
      ctx = JBUI.ScaleContext.create();
    }

    float width = myWidth * myScale;
    float height = myHeight * myScale;

    SVGDiagram targetDiagram = myX1Diagram;
    double jvmScale = 1f;
    if ((jvmScale = ctx.getScale(JBUI.ScaleType.SYS_SCALE)) > 1f) {
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

    JBHiDPIScaledImage image;

    // it's a dirty hack, due svg salamander don't have real scale support, and we don't want use batik
    // we will draw our icon with default size, and upscale with SPEED option
    boolean isOurLogo = myImageId != null && ICONS.contains(myImageId) && PLATFORM_ICON_GROUP.equals(myGroupId);
    if (isOurLogo) {
      image = new JBHiDPIScaledImage(1f, myWidth, myHeight, BufferedImage.TYPE_INT_ARGB, PaintUtil.RoundingMode.ROUND);
    }
    else {
      image = new JBHiDPIScaledImage(imageScale, width, height, BufferedImage.TYPE_INT_ARGB, PaintUtil.RoundingMode.ROUND);
    }

    Graphics2D g = image.createGraphics();
    paintIcon(targetDiagram, g, image.getUserWidth(null), image.getUserHeight(null));
    g.dispose();

    if (isOurLogo) {
      image = image.scale((int)width, (int)height, Scalr.Method.SPEED);
    }

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

    toPaintImage = ImageUtil.ensureHiDPI(toPaintImage, ctx);
    return toPaintImage;
  }

  private void paintIcon(SVGDiagram diagram, Graphics2D g, float width, float height) {
    GraphicsUtil.setupAAPainting(g);

    diagram.setDeviceViewport(new Rectangle((int)width, (int)height));

    diagram.setIgnoringClipHeuristic(true);

    try {
      diagram.render(g);
    }
    catch (SVGException e) {
      throw new RuntimeException(e);
    }
  }
}
