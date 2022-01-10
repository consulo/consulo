package consulo.ui.desktop.internal.image.canvas;

import com.intellij.util.ui.GraphicsUtil;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxLightweightLabel;
import consulo.awt.TargetAWT;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.style.StandardColors;
import org.imgscalr.Scalr;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Stack;

/**
 * Based on jgraphx, BSD license
 */
@SuppressWarnings("UseJBColor")
public class DesktopCanvas2DImpl implements Canvas2D {

  /**
   *
   */
  protected class CanvasState implements Cloneable {
    /**
     *
     */
    protected float myGlobalAlpha = 1;

    /**
     *
     */
    protected double scale = 1;

    /**
     *
     */
    protected double dx = 0;

    /**
     *
     */
    protected double dy = 0;

    /**
     *
     */
    protected double theta = 0;

    /**
     *
     */
    protected double rotationCx = 0;

    /**
     *
     */
    protected double rotationCy = 0;

    /**
     *
     */
    protected boolean flipV = false;

    /**
     *
     */
    protected boolean flipH = false;

    /**
     *
     */
    protected double miterLimit = 10;

    protected Font myFont = new Font("Arial", 0, 11);

    protected TextAlign myTextAlign;

    protected TextBaseline myTextBaseline;

    /**
     *
     */
    protected String lineCap = "flat";

    /**
     *
     */
    protected String lineJoin = "miter";

    /**
     *
     */
    protected double strokeWidth = 1;

    /**
     *
     */
    protected ColorValue strokeColorValue;

    /**
     *
     */
    protected Color strokeColor;

    /**
     *
     */
    protected ColorValue fillColorValue = StandardColors.BLACK;

    /**
     *
     */
    protected Color fillColor;

    /**
     *
     */
    protected Paint gradientPaint;

    /**
     *
     */
    protected boolean dashed = false;

    /**
     *
     */
    protected boolean fixDash = false;

    /**
     *
     */
    protected float[] dashPattern = {3, 3};

    /**
     *
     */
    protected boolean shadow = false;

    /**
     *
     */
    protected ColorValue shadowColorValue = StandardColors.GRAY;

    /**
     *
     */
    protected Color shadowColor;

    /**
     *
     */
    protected double shadowAlpha = 1;

    /**
     *
     */
    protected double shadowOffsetX = mxConstants.SHADOW_OFFSETX;

    /**
     *
     */
    protected double shadowOffsetY = mxConstants.SHADOW_OFFSETY;

    /**
     * Stores the actual state.
     */
    protected transient Graphics2D g;

    /**
     *
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }
  }

  private static final Logger log = Logger.getInstance(DesktopCanvas2DImpl.class);

  /**
   * Reference to the graphics instance for painting.
   */
  protected Graphics2D graphics;


  /**
   * Represents the current state of the canvas.
   */
  protected transient CanvasState state = new CanvasState();

  /**
   * Stack of states for save/restore.
   */
  protected transient Stack<CanvasState> stack = new Stack<>();

  /**
   * Holds the current path.
   */
  protected transient GeneralPath currentPath;

  /**
   * Stroke caching.
   */
  protected transient Stroke lastStroke = null;

  /**
   * Stroke caching.
   */
  protected transient float lastStrokeWidth = 0;

  /**
   * Stroke caching.
   */
  protected transient int lastCap = 0;

  /**
   * Stroke caching.
   */
  protected transient int lastJoin = 0;

  /**
   * Stroke caching.
   */
  protected transient float lastMiterLimit = 0;

  /**
   * Stroke caching.
   */
  protected transient boolean lastDashed = false;

  /**
   * Stroke caching.
   */
  protected transient Object lastDashPattern = "";

  /**
   * Constructs a new graphics export canvas.
   */
  public DesktopCanvas2DImpl(Graphics2D g) {
    graphics = g;
    state.g = g;
  }

  /**
   * Saves the current canvas state.
   */
  @Override
  public void save() {
    stack.push(state);
    state = cloneState(state);
    state.g = (Graphics2D)state.g.create();
  }

  /**
   * Restores the last canvas state.
   */
  @Override
  public void restore() {
    state.g.dispose();
    state = stack.pop();
  }

  /**
   * Returns a clone of the given state.
   */
  protected CanvasState cloneState(CanvasState state) {
    try {
      return (CanvasState)state.clone();
    }
    catch (CloneNotSupportedException e) {
      log.error(e);
    }

    return null;
  }

  /**
   *
   */
  @Override
  public void scale(double value) {
    // This implementation uses custom scale/translate and built-in rotation
    state.scale = state.scale * value;
  }

  /**
   *
   */
  @Override
  public void translate(double dx, double dy) {
    // This implementation uses custom scale/translate and built-in rotation
    state.dx += dx;
    state.dy += dy;
  }

  /**
   *
   */
  @Override
  public void rotate(double theta, boolean flipH, boolean flipV, double cx, double cy) {
    cx += state.dx;
    cy += state.dy;
    cx *= state.scale;
    cy *= state.scale;
    state.g.rotate(Math.toRadians(theta), cx, cy);

    // This implementation uses custom scale/translate and built-in rotation
    // Rotation state is part of the AffineTransform in state.transform
    if (flipH && flipV) {
      theta += 180;
    }
    else if (flipH ^ flipV) {
      double tx = (flipH) ? cx : 0;
      int sx = (flipH) ? -1 : 1;

      double ty = (flipV) ? cy : 0;
      int sy = (flipV) ? -1 : 1;

      state.g.translate(tx, ty);
      state.g.scale(sx, sy);
      state.g.translate(-tx, -ty);
    }

    state.theta = theta;
    state.rotationCx = cx;
    state.rotationCy = cy;
    state.flipH = flipH;
    state.flipV = flipV;
  }

  /**
   *
   */
  @Override
  public void setStrokeWidth(double value) {
    // Lazy and cached instantiation strategy for all stroke properties
    if (value != state.strokeWidth) {
      state.strokeWidth = value;
    }
  }

  /**
   * Caches color conversion as it is expensive.
   */
  @Override
  public void setStrokeStyle(ColorValue value) {
    // Lazy and cached instantiation strategy for all stroke properties
    if (state.strokeColorValue == null || !state.strokeColorValue.equals(value)) {
      state.strokeColorValue = value;
      state.strokeColor = null;
    }
  }

  /**
   *
   */
  @Override
  public void setDashed(boolean value) {
    this.setDashed(value, state.fixDash);
  }

  /**
   *
   */
  @Override
  public void setDashed(boolean value, boolean fixDash) {
    // Lazy and cached instantiation strategy for all stroke properties
    state.dashed = value;
    state.fixDash = fixDash;
  }

  /**
   *
   */
  @Override
  public void setDashPattern(String value) {
    if (value != null && value.length() > 0) {
      state.dashPattern = DesktopCanvas2DImplUtil.parseDashPattern(value);
    }
  }

  /**
   *
   */
  @Override
  public void setLineCap(String value) {
    if (!state.lineCap.equals(value)) {
      state.lineCap = value;
    }
  }

  /**
   *
   */
  @Override
  public void setLineJoin(String value) {
    if (!state.lineJoin.equals(value)) {
      state.lineJoin = value;
    }
  }

  /**
   *
   */
  @Override
  public void setMiterLimit(double value) {
    if (value != state.miterLimit) {
      state.miterLimit = value;
    }
  }

  @Override
  public void setFont(@Nonnull consulo.ui.font.Font font) {
    if (!font.equals(state.myFont)) {
      state.myFont = TargetAWT.to(font);
    }
  }

  @Override
  public void setTextAlign(@Nonnull TextAlign textAlign) {
    state.myTextAlign = textAlign;
  }

  @Override
  public void setTextBaseline(@Nonnull TextBaseline baseline) {
    state.myTextBaseline = baseline;
  }

  /**
   *
   */
  @Override
  public void setGlobalAlpha(float value) {
    if (state.myGlobalAlpha != value) {
      state.myGlobalAlpha = value;
    }
  }

  @Nonnull
  private AutoCloseable withAlpha(Graphics2D graphics) {
    if (state.myGlobalAlpha == 1) {
      return () -> {
      };
    }

    Composite old = graphics.getComposite();
    graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, state.myGlobalAlpha));
    return () -> graphics.setComposite(old);
  }

  /**
   *
   */
  @Override
  public void setFillStyle(ColorValue value) {
    if (state.fillColorValue == null || !state.fillColorValue.equals(value)) {
      state.fillColorValue = value;
      state.fillColor = null;

      // Setting fill color resets gradient paint
      state.gradientPaint = null;
    }
  }

  /**
   *
   */
  @Override
  public void setGradient(ColorValue color1, ColorValue color2, double x, double y, double w, double h, String direction, double alpha1, double alpha2) {
    // LATER: Add lazy instantiation and check if paint already created
    float x1 = (float)((state.dx + x) * state.scale);
    float y1 = (float)((state.dy + y) * state.scale);
    float x2 = x1;
    float y2 = y1;
    h *= state.scale;
    w *= state.scale;

    if (direction == null || direction.length() == 0 || direction.equals(mxConstants.DIRECTION_SOUTH)) {
      y2 = (float)(y1 + h);
    }
    else if (direction.equals(mxConstants.DIRECTION_EAST)) {
      x2 = (float)(x1 + w);
    }
    else if (direction.equals(mxConstants.DIRECTION_NORTH)) {
      y1 = (float)(y1 + h);
    }
    else if (direction.equals(mxConstants.DIRECTION_WEST)) {
      x1 = (float)(x1 + w);
    }

    Color c1 = convertColor(color1);

    if (alpha1 != 1) {
      c1 = new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), (int)(alpha1 * 255));
    }

    Color c2 = convertColor(color2);

    if (alpha2 != 1) {
      c2 = new Color(c2.getRed(), c2.getGreen(), c2.getBlue(), (int)(alpha2 * 255));
    }

    state.gradientPaint = new GradientPaint(x1, y1, c1, x2, y2, c2, true);

    // Resets fill color
    state.fillColorValue = null;
  }

  @Nonnull
  private Color convertColor(@Nonnull ColorValue colorValue) {
    RGBColor rgbColor = colorValue.toRGB();
    float[] floatValues = rgbColor.getFloatValues();
    return new Color(floatValues[0], floatValues[1], floatValues[2], floatValues[3]);
  }

  @Override
  public void rect(double x, double y, double w, double h) {
    currentPath = new GeneralPath();
    currentPath.append(new Rectangle2D.Double((state.dx + x) * state.scale, (state.dy + y) * state.scale, w * state.scale, h * state.scale), false);
  }

  @Override
  public void arc(double x, double y, double r, double sAngle, double eAngle) {
    // if we set x&y to zero - there will be only 1/4 circle - move it
    x -= r;
    y -= r;

    // TODO [VISTALL] sAngle + eAngle not supported
    currentPath = new GeneralPath();
    currentPath.append(new Ellipse2D.Double((state.dx + x) * state.scale, (state.dy + y) * state.scale, r * state.scale * 2, r * state.scale * 2), false);
  }

  @Override
  public void drawImage(@Nonnull Image image, double x, double y, double w, double h) {
    drawImageImpl(image, x, y, w, h, false, false, false);
  }

  public void drawImageImpl(@Nonnull Image src, double x, double y, double w, double h, boolean aspect, boolean flipH, boolean flipV) {
    if (w > 0 && h > 0) {
      Icon icon = TargetAWT.to(src);

      Rectangle bounds = getImageBounds(icon, x, y, w, h, aspect);

      Graphics2D graphics = createImageGraphics(bounds.x, bounds.y, bounds.width, bounds.height, flipH, flipV);

      try(AutoCloseable ignored = withAlpha(graphics)) {
        if (icon.getIconHeight() == bounds.height && icon.getIconWidth() == bounds.width) {
          icon.paintIcon(mxLightweightLabel.getSharedInstance(), graphics, bounds.x, bounds.y);
        }
        else {
          BufferedImage resizedImage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
          icon.paintIcon(mxLightweightLabel.getSharedInstance(), resizedImage.getGraphics(), 0, 0);
          resizedImage = Scalr.resize(resizedImage, Scalr.Method.QUALITY, bounds.width, bounds.height);
          graphics.drawImage(resizedImage, null, bounds.x, bounds.y);
        }
      }
      catch (Exception ignored) {
      }
    }
  }

  /**
   *
   */
  protected final Rectangle getImageBounds(Icon img, double x, double y, double w, double h, boolean aspect) {
    x = (state.dx + x) * state.scale;
    y = (state.dy + y) * state.scale;
    w *= state.scale;
    h *= state.scale;

    if (aspect) {
      Dimension size = getImageSize(img);
      double s = Math.min(w / size.width, h / size.height);
      int sw = (int)Math.round(size.width * s);
      int sh = (int)Math.round(size.height * s);
      x += (w - sw) / 2;
      y += (h - sh) / 2;
      w = sw;
      h = sh;
    }
    else {
      w = Math.round(w);
      h = Math.round(h);
    }

    return new Rectangle((int)x, (int)y, (int)w, (int)h);
  }

  /**
   * Returns the size for the given image.
   */
  protected Dimension getImageSize(Icon image) {
    return new Dimension(image.getIconWidth(), image.getIconHeight());
  }

  /**
   * Creates a graphic instance for rendering an image.
   */
  protected final Graphics2D createImageGraphics(double x, double y, double w, double h, boolean flipH, boolean flipV) {
    Graphics2D g2 = state.g;

    if (flipH || flipV) {
      g2 = (Graphics2D)g2.create();
      GraphicsUtil.setupAAPainting(g2);

      if (flipV && flipH) {
        g2.rotate(Math.toRadians(180), x + w / 2, y + h / 2);
      }
      else {
        int sx = 1;
        int sy = 1;
        int dx = 0;
        int dy = 0;

        if (flipH) {
          sx = -1;
          dx = (int)(-w - 2 * x);
        }

        if (flipV) {
          sy = -1;
          dy = (int)(-h - 2 * y);
        }

        g2.scale(sx, sy);
        g2.translate(dx, dy);
      }
    }

    return g2;
  }

  protected Point2D getMargin(TextAlign align, TextBaseline valign) {
    double dx = 0;
    double dy = 0;

    if (align != null) {
      if (align == TextAlign.center) {
        dx = -0.5;
      }
      else if (align == TextAlign.right) {
        dx = -1;
      }
    }

    if (valign != null) {
      if (valign == TextBaseline.middle) {
        dy = -0.5;
      }
      else if (valign == TextBaseline.bottom) {
        dy = -1;
      }
    }

    return new Point2D.Double(dx, dy);
  }

  @Override
  public void fillText(String text, double x, double y, double maxWidth) {
    text(x, y, 0, 0, text, state.myTextAlign, state.myTextBaseline, false, 0);
  }

  /**
   * Draws the given text.
   */
  private void text(double x, double y, double w, double h, String str, TextAlign align, TextBaseline valign, boolean clip, double rotation) {
    if (state.fillColor == null) {
      state.fillColor = convertColor(state.fillColorValue);
    }

    if (state.fillColor != null) {
      x = (state.dx + x) * state.scale;
      y = (state.dy + y) * state.scale;
      w *= state.scale;
      h *= state.scale;

      // Font-metrics needed below this line
      Graphics2D g2 = createTextGraphics(x, y, w, h, rotation, clip, align, valign);
      FontMetrics fm = g2.getFontMetrics();
      String[] lines = str.split("\n");

      int[] stringWidths = new int[lines.length];
      int textWidth = 0;

      for (int i = 0; i < lines.length; i++) {
        stringWidths[i] = fm.stringWidth(lines[i]);
        textWidth = Math.max(textWidth, stringWidths[i]);
      }

      int textHeight = Math.round(lines.length * (fm.getFont().getSize() * mxConstants.LINE_HEIGHT));

      if (clip && textHeight > h && h > 0) {
        textHeight = (int)h;
      }

      Point2D margin = getMargin(align, valign);
      x += margin.getX() * textWidth;
      y += margin.getY() * textHeight;

      g2.setColor(state.fillColor);
      y += fm.getHeight() - fm.getDescent() - (margin.getY() + 0.5);

      for (int i = 0; i < lines.length; i++) {
        double dx = 0;

        if (align != null) {
          if (align == TextAlign.center) {
            dx = (textWidth - stringWidths[i]) / 2;
          }
          else if (align == TextAlign.right) {
            dx = textWidth - stringWidths[i];
          }
        }

        if (!lines[i].isEmpty()) {
          g2.drawString(lines[i], (int)Math.round(x + dx), (int)Math.round(y));
        }

        y += Math.round(fm.getFont().getSize() * mxConstants.LINE_HEIGHT);
      }
    }
  }

  /**
   * Returns a new graphics instance with the correct color and font for
   * text rendering.
   */
  protected final Graphics2D createTextGraphics(double x, double y, double w, double h, double rotation, boolean clip, TextAlign align, TextBaseline valign) {
    Graphics2D g2 = state.g;
    state.g.setFont(state.myFont);

    if (rotation != 0) {
      g2 = (Graphics2D)state.g.create();

      double rad = rotation * (Math.PI / 180);
      g2.rotate(rad, x, y);
    }

    if (clip && w > 0 && h > 0) {
      if (g2 == state.g) {
        g2 = (Graphics2D)state.g.create();
      }

      Point2D margin = getMargin(align, valign);
      x += margin.getX() * w;
      y += margin.getY() * h;

      g2.clip(new Rectangle2D.Double(x, y, w, h));
    }

    return g2;
  }

  /**
   *
   */
  @Override
  public void beginPath() {
    currentPath = new GeneralPath();
  }

  /**
   *
   */
  @Override
  public void moveTo(double x, double y) {
    if (currentPath != null) {
      currentPath.moveTo((float)((state.dx + x) * state.scale), (float)((state.dy + y) * state.scale));
    }
  }

  /**
   *
   */
  @Override
  public void lineTo(double x, double y) {
    if (currentPath != null) {
      currentPath.lineTo((float)((state.dx + x) * state.scale), (float)((state.dy + y) * state.scale));
    }
  }

  /**
   *
   */
  @Override
  public void quadraticCurveTo(double x1, double y1, double x2, double y2) {
    if (currentPath != null) {
      currentPath.quadTo((float)((state.dx + x1) * state.scale), (float)((state.dy + y1) * state.scale), (float)((state.dx + x2) * state.scale), (float)((state.dy + y2) * state.scale));
    }
  }

  /**
   *
   */
  @Override
  public void curveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
    if (currentPath != null) {
      currentPath.curveTo((float)((state.dx + x1) * state.scale), (float)((state.dy + y1) * state.scale), (float)((state.dx + x2) * state.scale), (float)((state.dy + y2) * state.scale),
                          (float)((state.dx + x3) * state.scale), (float)((state.dy + y3) * state.scale));
    }
  }

  /**
   * Closes the current path.
   */
  @Override
  public void closePath() {
    if (currentPath != null) {
      currentPath.closePath();
    }
  }

  /**
   *
   */
  @Override
  public void stroke() {
    paintCurrentPath(false, true);
  }

  /**
   *
   */
  @Override
  public void fill() {
    paintCurrentPath(true, false);
  }

  /**
   *
   */
  protected void paintCurrentPath(boolean filled, boolean stroked) {
    if (currentPath != null) {
      if (stroked) {
        if (state.strokeColor == null) {
          state.strokeColor = convertColor(state.strokeColorValue);
        }

        if (state.strokeColor != null) {
          updateStroke();
        }
      }

      if (filled) {
        if (state.gradientPaint == null && state.fillColor == null) {
          state.fillColor = convertColor(state.fillColorValue);
        }
      }

      if (state.shadow) {
        paintShadow(filled, stroked);
      }

      if (filled) {
        if (state.gradientPaint != null) {
          state.g.setPaint(state.gradientPaint);
          state.g.fill(currentPath);
        }
        else {
          if (state.fillColor != null) {
            state.g.setColor(state.fillColor);
            state.g.setPaint(null);
            state.g.fill(currentPath);
          }
        }
      }

      if (stroked && state.strokeColor != null) {
        state.g.setColor(state.strokeColor);
        state.g.draw(currentPath);
      }
    }
  }

  /**
   *
   */
  protected void paintShadow(boolean filled, boolean stroked) {
    if (state.shadowColor == null) {
      state.shadowColor = convertColor(state.shadowColorValue);
    }

    if (state.shadowColor != null) {
      double rad = -state.theta * (Math.PI / 180);
      double cos = Math.cos(rad);
      double sin = Math.sin(rad);

      double dx = state.shadowOffsetX * state.scale;
      double dy = state.shadowOffsetY * state.scale;

      if (state.flipH) {
        dx *= -1;
      }

      if (state.flipV) {
        dy *= -1;
      }

      double tx = dx * cos - dy * sin;
      double ty = dx * sin + dy * cos;

      state.g.setColor(state.shadowColor);
      state.g.translate(tx, ty);

      double alpha = state.myGlobalAlpha * state.shadowAlpha;

      Composite comp = state.g.getComposite();
      state.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(alpha)));

      if (filled && (state.gradientPaint != null || state.fillColor != null)) {
        state.g.fill(currentPath);
      }

      // FIXME: Overlaps with fill in composide mode
      if (stroked && state.strokeColor != null) {
        state.g.draw(currentPath);
      }

      state.g.translate(-tx, -ty);
      state.g.setComposite(comp);
    }
  }

  /**
   *
   */
  @Override
  public void setShadow(boolean value) {
    state.shadow = value;
  }

  /**
   *
   */
  @Override
  public void setShadowColor(ColorValue value) {
    state.shadowColorValue = value;
  }

  /**
   *
   */
  @Override
  public void setShadowAlpha(double value) {
    state.shadowAlpha = value;
  }

  /**
   *
   */
  @Override
  public void setShadowOffset(double dx, double dy) {
    state.shadowOffsetX = dx;
    state.shadowOffsetY = dy;
  }
  /**
   *
   */
  protected void updateStroke() {
    float sw = (float)Math.max(1, state.strokeWidth * state.scale);
    int cap = BasicStroke.CAP_BUTT;

    if (state.lineCap.equals("round")) {
      cap = BasicStroke.CAP_ROUND;
    }
    else if (state.lineCap.equals("square")) {
      cap = BasicStroke.CAP_SQUARE;
    }

    int join = BasicStroke.JOIN_MITER;

    if (state.lineJoin.equals("round")) {
      join = BasicStroke.JOIN_ROUND;
    }
    else if (state.lineJoin.equals("bevel")) {
      join = BasicStroke.JOIN_BEVEL;
    }

    float miterlimit = (float)state.miterLimit;

    if (lastStroke == null ||
        lastStrokeWidth != sw ||
        lastCap != cap ||
        lastJoin != join ||
        lastMiterLimit != miterlimit ||
        lastDashed != state.dashed ||
        (state.dashed && lastDashPattern != state.dashPattern)) {
      float[] dash = null;

      if (state.dashed) {
        dash = new float[state.dashPattern.length];

        for (int i = 0; i < dash.length; i++) {
          dash[i] = (float)(state.dashPattern[i] * ((state.fixDash) ? state.scale : sw));
        }
      }

      lastStroke = new BasicStroke(sw, cap, join, miterlimit, dash, 0);
      lastStrokeWidth = sw;
      lastCap = cap;
      lastJoin = join;
      lastMiterLimit = miterlimit;
      lastDashed = state.dashed;
      lastDashPattern = state.dashPattern;
    }

    state.g.setStroke(lastStroke);
  }
}