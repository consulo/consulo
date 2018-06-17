package consulo.ui.internal.image.canvas;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.SizedIcon;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxLightweightLabel;
import consulo.awt.TargetAWT;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.shared.ColorValue;
import consulo.ui.shared.RGBColor;
import consulo.ui.style.StandardColors;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.text.AttributedString;
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
    protected double alpha = 1;

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

    /**
     *
     */
    protected int fontStyle = 0;

    /**
     *
     */
    protected double fontSize = mxConstants.DEFAULT_FONTSIZE;

    /**
     *
     */
    protected String fontFamily = mxConstants.DEFAULT_FONTFAMILIES;

    /**
     *
     */
    protected ColorValue fontColorValue = StandardColors.BLACK;

    /**
     *
     */
    protected Color fontColor;

    /**
     *
     */
    protected ColorValue fontBackgroundColorValue;

    /**
     *
     */
    protected Color fontBackgroundColor;

    /**
     *
     */
    protected ColorValue fontBorderColorValue;

    /**
     *
     */
    protected Color fontBorderColor;

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
    protected ColorValue fillColorValue;

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

  /**
   * Specifies if absolute line heights should be used (px) in CSS. Default
   * is false. Set this to true for backwards compatibility.
   */
  public static boolean ABSOLUTE_LINE_HEIGHT = false;

  private static final Logger log = Logger.getInstance(DesktopCanvas2DImpl.class);

  /**
   * Specifies the additional pixels when computing the text width for HTML labels.
   * Default is 5.
   */
  public static int JAVA_TEXT_WIDTH_DELTA = 6;

  /**
   * Scale for rendering HTML output. Default is 1.
   */
  public static double HTML_SCALE = 1;

  /**
   * Unit to be used for HTML labels. Default is "pt". If you units within
   * HTML labels are used, this should match those units to produce a
   * consistent output. If the value is "px", then HTML_SCALE should be
   * changed the match the ratio between px units for rendering HTML and
   * the units used for rendering other graphics elements. This value is
   * 0.6 on Linux and 0.75 on all other platforms.
   */
  public static String HTML_UNIT = "pt";


  /**
   * Reference to the graphics instance for painting.
   */
  protected Graphics2D graphics;

  /**
   * Specifies if text output should be rendered. Default is true.
   */
  protected boolean textEnabled = true;

  /**
   * Represents the current state of the canvas.
   */
  protected transient CanvasState state = new CanvasState();

  /**
   * Stack of states for save/restore.
   */
  protected transient Stack<CanvasState> stack = new Stack<CanvasState>();

  /**
   * Holds the current path.
   */
  protected transient GeneralPath currentPath;

  /**
   * Optional renderer pane to be used for HTML label rendering.
   */
  protected CellRendererPane rendererPane;

  /**
   * Font caching.
   */
  protected transient Font lastFont = null;

  /**
   * Font caching.
   */
  protected transient int lastFontStyle = 0;

  /**
   * Font caching.
   */
  protected transient int lastFontSize = 0;

  /**
   * Font caching.
   */
  protected transient String lastFontFamily = "";

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
    setGraphics(g);
    state.g = g;

    // Initializes the cell renderer pane for drawing HTML markup
    try {
      rendererPane = new CellRendererPane();
    }
    catch (Exception e) {
      log.error(e);
    }
  }

  /**
   * Sets the graphics instance.
   */
  public void setGraphics(Graphics2D value) {
    graphics = value;
  }

  /**
   * Returns the graphics instance.
   */
  public Graphics2D getGraphics() {
    return graphics;
  }

  /**
   * Returns true if text should be rendered.
   */
  public boolean isTextEnabled() {
    return textEnabled;
  }

  /**
   * Disables or enables text rendering.
   */
  public void setTextEnabled(boolean value) {
    textEnabled = value;
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

  /**
   *
   */
  @Override
  public void setFontSize(double value) {
    if (value != state.fontSize) {
      state.fontSize = value;
    }
  }

  /**
   *
   */
  @Override
  public void setFontColor(ColorValue value) {
    if (state.fontColorValue == null || !state.fontColorValue.equals(value)) {
      state.fontColorValue = value;
      state.fontColor = null;
    }
  }

  /**
   *
   */
  @Override
  public void setFontBackgroundColor(ColorValue value) {
    if (state.fontBackgroundColorValue == null || !state.fontBackgroundColorValue.equals(value)) {
      state.fontBackgroundColorValue = value;
      state.fontBackgroundColor = null;
    }
  }

  /**
   *
   */
  @Override
  public void setFontBorderColor(ColorValue value) {
    if (state.fontBorderColorValue == null || !state.fontBorderColorValue.equals(value)) {
      state.fontBorderColorValue = value;
      state.fontBorderColor = null;
    }
  }

  /**
   *
   */
  @Override
  public void setFontFamily(String value) {
    if (!state.fontFamily.equals(value)) {
      state.fontFamily = value;
    }
  }

  /**
   *
   */
  @Override
  public void setFontStyle(int value) {
    if (value != state.fontStyle) {
      state.fontStyle = value;
    }
  }

  /**
   *
   */
  @Override
  public void setAlpha(double value) {
    if (state.alpha != value) {
      state.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(value)));
      state.alpha = value;
    }
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
    float x2 = (float)x1;
    float y2 = (float)y1;
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
  private Color convertColor(ColorValue colorValue) {
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
  public void image(double x, double y, double w, double h, @Nonnull consulo.ui.image.Image src, boolean aspect, boolean flipH, boolean flipV) {
    if (w > 0 && h > 0) {
      Icon icon = TargetAWT.to(src);

      Rectangle bounds = getImageBounds(icon, x, y, w, h, aspect);
      icon = scaleImage(icon, bounds.width, bounds.height);

      Graphics2D graphics = createImageGraphics(bounds.x, bounds.y, bounds.width, bounds.height, flipH, flipV);

      icon.paintIcon(mxLightweightLabel.getSharedInstance(), graphics, bounds.x, bounds.y);
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

  protected Icon scaleImage(Icon img, int w, int h) {
    Dimension size = getImageSize(img);

    if (w == size.width && h == size.height) {
      return img;
    }
    else {
      return new SizedIcon(img, w, h);
    }
  }

  /**
   * Creates a graphic instance for rendering an image.
   */
  protected final Graphics2D createImageGraphics(double x, double y, double w, double h, boolean flipH, boolean flipV) {
    Graphics2D g2 = state.g;

    if (flipH || flipV) {
      g2 = (Graphics2D)g2.create();

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

  /**
   * Creates a HTML document around the given markup.
   */
  protected String createHtmlDocument(String text, String align, String valign, int w, int h, boolean wrap, String overflow, boolean clip) {
    StringBuffer css = new StringBuffer();
    css.append("display:inline;");
    css.append("font-family:" + state.fontFamily + ";");
    css.append("font-size:" + Math.round(state.fontSize) + HTML_UNIT + ";");
    css.append("color:" + state.fontColorValue + ";");
    // KNOWN: Line-height ignored in JLabel
    css.append("line-height:" + (ABSOLUTE_LINE_HEIGHT ? Math.round(state.fontSize * mxConstants.LINE_HEIGHT) + " " + HTML_UNIT : mxConstants.LINE_HEIGHT) + ";");

    boolean setWidth = false;

    if ((state.fontStyle & mxConstants.FONT_BOLD) == mxConstants.FONT_BOLD) {
      css.append("font-weight:bold;");
    }

    if ((state.fontStyle & mxConstants.FONT_ITALIC) == mxConstants.FONT_ITALIC) {
      css.append("font-style:italic;");
    }

    if ((state.fontStyle & mxConstants.FONT_UNDERLINE) == mxConstants.FONT_UNDERLINE) {
      css.append("text-decoration:underline;");
    }

    if (align != null) {
      if (align.equals(mxConstants.ALIGN_CENTER)) {
        css.append("text-align:center;");
      }
      else if (align.equals(mxConstants.ALIGN_RIGHT)) {
        css.append("text-align:right;");
      }
    }

    if (state.fontBackgroundColorValue != null) {
      css.append("background-color:" + state.fontBackgroundColorValue + ";");
    }

    // KNOWN: Border ignored in JLabel
    if (state.fontBorderColorValue != null) {
      css.append("border:1pt solid " + state.fontBorderColorValue + ";");
    }

    // KNOWN: max-width/-height ignored in JLabel
    if (clip) {
      css.append("overflow:hidden;");
      setWidth = true;
    }
    else if (overflow != null) {
      if (overflow.equals("fill")) {
        css.append("height:" + Math.round(h) + HTML_UNIT + ";");
        setWidth = true;
      }
      else if (overflow.equals("width")) {
        setWidth = true;

        if (h > 0) {
          css.append("height:" + Math.round(h) + HTML_UNIT + ";");
        }
      }
    }

    if (wrap) {
      if (!clip) {
        // NOTE: Max-width not available in Java
        setWidth = true;
      }

      css.append("white-space:normal;");
    }
    else {
      css.append("white-space:nowrap;");
    }

    if (setWidth && w > 0) {
      css.append("width:" + Math.round(w) + HTML_UNIT + ";");
    }

    return createHtmlDocument(text, css.toString());
  }

  /**
   * Creates a HTML document for the given text and CSS style.
   */
  protected String createHtmlDocument(String text, String style) {
    return "<html><div style=\"" + style + "\">" + text + "</div></html>";
  }

  /**
   * Hook to return the renderer for HTML formatted text. This implementation returns
   * the shared instance of mxLighweightLabel.
   */
  protected JLabel getTextRenderer() {
    return mxLightweightLabel.getSharedInstance();
  }

  /**
   *
   */
  protected Point2D getMargin(String align, String valign) {
    double dx = 0;
    double dy = 0;

    if (align != null) {
      if (align.equals(mxConstants.ALIGN_CENTER)) {
        dx = -0.5;
      }
      else if (align.equals(mxConstants.ALIGN_RIGHT)) {
        dx = -1;
      }
    }

    if (valign != null) {
      if (valign.equals(mxConstants.ALIGN_MIDDLE)) {
        dy = -0.5;
      }
      else if (valign.equals(mxConstants.ALIGN_BOTTOM)) {
        dy = -1;
      }
    }

    return new Point2D.Double(dx, dy);
  }

  /**
   * Draws the given HTML text.
   */
  protected void htmlText(double x, double y, double w, double h, String str, String align, String valign, boolean wrap, String format, String overflow, boolean clip, double rotation) {
    x += state.dx;
    y += state.dy;

    JLabel textRenderer = getTextRenderer();

    if (textRenderer != null && rendererPane != null) {
      // Use native scaling for HTML
      AffineTransform previous = state.g.getTransform();
      state.g.scale(state.scale * HTML_SCALE, state.scale * HTML_SCALE);
      double rad = rotation * (Math.PI / 180);
      state.g.rotate(rad, x, y);

      // Renders the scaled text with a correction factor
      // HTML_SCALE for the given HTML_UNIT
      boolean widthFill = false;
      boolean fill = false;

      String original = str;

      if (overflow != null) {
        widthFill = overflow.equals("width");
        fill = overflow.equals("fill");
      }

      str = createHtmlDocument(str, align, valign, (widthFill || fill) ? (int)Math.round(w) : 0, (fill) ? (int)Math.round(h) : 0, wrap, overflow, clip);
      textRenderer.setText(str);
      Dimension pref = textRenderer.getPreferredSize();
      int prefWidth = pref.width;
      int prefHeight = pref.height;

      // Poor man's max-width
      // TODO: Is this still needed?
      if (((clip || wrap) && prefWidth > w / HTML_SCALE && w > 0) || (clip && prefHeight > h / HTML_SCALE && h > 0)) {
        // TextWidthDelta is workaround for inconsistent word wrapping in Java
        int cw = (int)Math.round((w) + ((wrap) ? JAVA_TEXT_WIDTH_DELTA : 0));
        int ch = (int)Math.round(h);
        str = createHtmlDocument(original, align, valign, cw, ch, wrap, overflow, clip);
        textRenderer.setText(str);

        pref = textRenderer.getPreferredSize();
        prefWidth = pref.width;
        prefHeight = pref.height + 2;
      }

      // Matches HTML output
      if (clip && w > 0 && h > 0) {
        prefWidth = Math.min(pref.width, (int)(w / HTML_SCALE));
        prefHeight = Math.min(prefHeight, (int)(h / HTML_SCALE));
        h = prefHeight * HTML_SCALE;
      }
      else if (!clip && wrap && w > 0 && h > 0) {
        prefWidth = pref.width;
        w = Math.max(pref.width, (int)(w / HTML_SCALE));
        h = prefHeight * HTML_SCALE;
        prefHeight = Math.max(prefHeight, (int)(h / HTML_SCALE));
      }
      else if (!clip && !wrap) {
        if (w > 0 && w / HTML_SCALE < prefWidth) {
          w = prefWidth * HTML_SCALE;
        }

        if (h > 0 && h / HTML_SCALE < prefHeight) {
          h = prefHeight * HTML_SCALE;
        }
      }

      Point2D margin = getMargin(align, valign);
      x += margin.getX() * prefWidth * HTML_SCALE;
      y += margin.getY() * prefHeight * HTML_SCALE;

      if (w == 0) {
        w = prefWidth * HTML_SCALE;
      }

      if (h == 0) {
        h = prefHeight * HTML_SCALE;
      }

      rendererPane
              .paintComponent(state.g, textRenderer, rendererPane, (int)Math.round(x / HTML_SCALE), (int)Math.round(y / HTML_SCALE), (int)Math.round(w / HTML_SCALE), (int)Math.round(h / HTML_SCALE),
                              true);

      state.g.setTransform(previous);
    }
  }

  @Override
  public void fillText(String text, double x, double y, double maxWidth) {
    text(x, y, 0, 0, text, null, null, false, null, null, false, 0, null);
  }

  /**
   * Draws the given text.
   */
  private void text(double x,
                    double y,
                    double w,
                    double h,
                    String str,
                    String align,
                    String valign,
                    boolean wrap,
                    String format,
                    String overflow,
                    boolean clip,
                    double rotation,
                    String textDirection) {
    // TODO: Add support for text direction
    if (format != null && format.equals("html")) {
      htmlText(x, y, w, h, str, align, valign, wrap, format, overflow, clip, rotation);
    }
    else {
      plainText(x, y, w, h, str, align, valign, wrap, format, overflow, clip, rotation);
    }
  }

  /**
   * Draws the given text.
   */
  public void plainText(double x, double y, double w, double h, String str, String align, String valign, boolean wrap, String format, String overflow, boolean clip, double rotation) {
    if (state.fontColor == null) {
      state.fontColor = convertColor(state.fontColorValue);
    }

    if (state.fontColor != null) {
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

      int textHeight = (int)Math.round(lines.length * (fm.getFont().getSize() * mxConstants.LINE_HEIGHT));

      if (clip && textHeight > h && h > 0) {
        textHeight = (int)h;
      }

      Point2D margin = getMargin(align, valign);
      x += margin.getX() * textWidth;
      y += margin.getY() * textHeight;

      if (state.fontBackgroundColorValue != null) {
        if (state.fontBackgroundColor == null) {
          state.fontBackgroundColor = convertColor(state.fontBackgroundColorValue);
        }

        if (state.fontBackgroundColor != null) {
          g2.setColor(state.fontBackgroundColor);
          g2.fillRect((int)Math.round(x), (int)Math.round(y - 1), textWidth + 1, textHeight + 2);
        }
      }

      if (state.fontBorderColorValue != null) {
        if (state.fontBorderColor == null) {
          state.fontBorderColor = convertColor(state.fontBorderColorValue);
        }

        if (state.fontBorderColor != null) {
          g2.setColor(state.fontBorderColor);
          g2.drawRect((int)Math.round(x), (int)Math.round(y - 1), textWidth + 1, textHeight + 2);
        }
      }

      g2.setColor(state.fontColor);
      y += fm.getHeight() - fm.getDescent() - (margin.getY() + 0.5);

      for (int i = 0; i < lines.length; i++) {
        double dx = 0;

        if (align != null) {
          if (align.equals(mxConstants.ALIGN_CENTER)) {
            dx = (textWidth - stringWidths[i]) / 2;
          }
          else if (align.equals(mxConstants.ALIGN_RIGHT)) {
            dx = textWidth - stringWidths[i];
          }
        }

        // Adds support for underlined text via attributed character iterator
        if (!lines[i].isEmpty()) {
          if ((state.fontStyle & mxConstants.FONT_UNDERLINE) == mxConstants.FONT_UNDERLINE) {
            AttributedString as = new AttributedString(lines[i]);
            as.addAttribute(TextAttribute.FONT, g2.getFont());
            as.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);

            g2.drawString(as.getIterator(), (int)Math.round(x + dx), (int)Math.round(y));
          }
          else {
            g2.drawString(lines[i], (int)Math.round(x + dx), (int)Math.round(y));
          }
        }

        y += (int)Math.round(fm.getFont().getSize() * mxConstants.LINE_HEIGHT);
      }
    }
  }

  /**
   * Returns a new graphics instance with the correct color and font for
   * text rendering.
   */
  protected final Graphics2D createTextGraphics(double x, double y, double w, double h, double rotation, boolean clip, String align, String valign) {
    Graphics2D g2 = state.g;
    updateFont();

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

      double alpha = state.alpha * state.shadowAlpha;

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
  protected void updateFont() {
    int size = (int)Math.round(state.fontSize * state.scale);
    int style = ((state.fontStyle & mxConstants.FONT_BOLD) == mxConstants.FONT_BOLD) ? Font.BOLD : Font.PLAIN;
    style += ((state.fontStyle & mxConstants.FONT_ITALIC) == mxConstants.FONT_ITALIC) ? Font.ITALIC : Font.PLAIN;

    if (lastFont == null || !lastFontFamily.equals(state.fontFamily) || size != lastFontSize || style != lastFontStyle) {
      lastFont = createFont(state.fontFamily, style, size);
      lastFontFamily = state.fontFamily;
      lastFontStyle = style;
      lastFontSize = size;
    }

    state.g.setFont(lastFont);
  }

  /**
   * Hook for subclassers to implement font caching.
   */
  protected Font createFont(String family, int style, int size) {
    return new Font(getFontName(family), style, size);
  }

  /**
   * Returns a font name for the given CSS values for font-family.
   * This implementation returns the first entry for comma-separated
   * lists of entries.
   */
  protected String getFontName(String family) {
    if (family != null) {
      int comma = family.indexOf(',');

      if (comma >= 0) {
        family = family.substring(0, comma);
      }
    }

    return family;
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