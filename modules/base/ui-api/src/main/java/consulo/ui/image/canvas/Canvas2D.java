package consulo.ui.image.canvas;

import consulo.ui.font.Font;
import consulo.ui.image.Image;
import consulo.ui.color.ColorValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Based on jgraphx, BSD license
 */
public interface Canvas2D {
  enum TextAlign {
    left,
    center,
    right
  }

  enum TextBaseline {
    top,
    middle,
    bottom
  }

  void setGlobalAlpha(float value);

  void setFont(@Nonnull Font font);

  void setFillStyle(@Nullable ColorValue value);

  /**
   * Sets the stroke color.
   */
  void setStrokeStyle(@Nullable ColorValue value);

  void setTextAlign(@Nonnull TextAlign textAlign);

  void setTextBaseline(@Nonnull TextBaseline baseline);

  /**
   * Saves the current state of the canvas.
   */
  void save();

  /**
   * Restores the previous state of the canvas.
   */
  void restore();

  /**
   * Next fill or stroke should draw a rectangle.
   */
  void rect(double x, double y, double w, double h);

  /**
   * Next fill or stroke should draw a round rectangle.
   */
  default void roundRect(int x, int y, int width, int height, int radius) {
    Canvas2DHelper.roundRectangle(this, x, y, width, height, radius);
  }

  /**
   * Next fill should draw a rectangle.
   */
  default void fillRect(double x, double y, double w, double h) {
    rect(x, y, w, h);
    fill();
  }

  /**
   * Next fill should draw a rectangle.
   */
  default void strokeRect(double x, double y, double w, double h) {
    rect(x, y, w, h);
    stroke();
  }

  /**
   * Next fill or stroke should draw an arc/curve (used to create circles, or parts of circles)
   */
  void arc(double x, double y, double r, double sAngle, double eAngle);

  /**
   * Draws the given image.
   */
  default void drawImage(@Nonnull Image image, double x, double y) {
    drawImage(image, x, y, image.getWidth(), image.getHeight());
  }

  /**
   * Draws the given image.
   */
  void drawImage(@Nonnull Image image, double x, double y, double w, double h);

  /**
   * Draws the given string.
   */
  default void fillText(String text, double x, double y) {
    fillText(text, x, y, 0);
  }

  /**
   * Draws the given string.
   */
  void fillText(String text, double x, double y, double maxWidth);

  /**
   * Begins a new path.
   */
  void beginPath();

  /**
   * Moves to the given path.
   */
  void moveTo(double x, double y);

  /**
   * Draws a line to the given path.
   */
  void lineTo(double x, double y);

  /**
   * Draws a quadratic curve to the given point.
   */
  void quadraticCurveTo(double x1, double y1, double x2, double y2);

  /**
   * Draws a bezier curve to the given point.
   */
  void curveTo(double x1, double y1, double x2, double y2, double x3, double y3);

  /**
   * Closes the current path.
   */
  void closePath();

  /**
   * Paints the outline of the current path.
   */
  void stroke();

  /**
   * Fills the current path.
   */
  void fill();

  // region unsupported

  /**
   * Uniformaly scales the canvas by the given amount.
   *
   * @param value The new scale value.
   */
  void scale(double value);

  /**
   * Translates the canvas by the given amount.
   *
   * @param dx X-coordinate of the translation.
   * @param dy Y-coordinate of the translation.
   */
  void translate(double dx, double dy);

  /**
   * Rotates the canvas by the given angle around the given center. This
   * method may add rendering overhead and should be used with care.
   *
   * @param theta Rotation angle in degrees (0 - 360).
   * @param flipH Specifies if drawing should be flipped horizontally.
   * @param flipV Specifies if drawing should be flipped vertically.
   * @param cx    X-coordinate of the center point.
   * @param cy    Y-coordinate of the center point.
   */
  void rotate(double theta, boolean flipH, boolean flipV, double cx, double cy);

  /**
   * Sets the stroke width. This should default to 1 if unset.
   *
   * @param value Width of the stroke. The value should be multiplied by the
   *              current scale.
   */
  void setStrokeWidth(double value);

  /**
   * Sets the dashed state. This should default to false if unset.
   *
   * @param value Boolean representing the dashed state.
   */
  void setDashed(boolean value);

  /**
   * Sets the dashed state. This should default to false if unset.
   *
   * @param value Boolean representing the dashed state.
   */
  void setDashed(boolean value, boolean fixDash);

  /**
   * Sets the dash pattern. This should default to "3 3" if unset.
   *
   * @param value Space separated list of floats representing the dash
   *              pattern. The value should be multiplied by the current scale.
   */
  void setDashPattern(String value);

  /**
   * Sets the linecap. This should default to "flat" if unset.
   *
   * @param value "flat", "square" or "round".
   */
  void setLineCap(String value);

  /**
   * Sets the linejoin. This should default to "miter" if unset.
   *
   * @param value "miter", "round" or "bevel".
   */
  void setLineJoin(String value);

  void setMiterLimit(double value);

  /**
   * Prepares the canvas to draw a gradient.
   */
  void setGradient(ColorValue color1, ColorValue color2, double x, double y, double w, double h, String direction, double alpha1, double alpha2);

  /**
   * Enables or disables the painting of shadows.
   *
   * @param enabled Whether the shadow should be enabled.
   */
  void setShadow(boolean enabled);

  void setShadowColor(ColorValue value);

  void setShadowAlpha(double value);

  void setShadowOffset(double dx, double dy);
  // endregion
}