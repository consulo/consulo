package consulo.ui.util;

import consulo.annotation.UsedInPlugin;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-06-12
 */
@UsedInPlugin
public class ColorValueUtil {
  /**
   * Return Color object from string. The following formats are allowed:
   * <code>#abc123</code>,
   * <code>ABC123</code>,
   * <code>ab5</code>,
   * <code>#FFF</code>.
   *
   * @param str hex string
   * @return RGBColor object
   */
  @Nonnull
  public static RGBColor fromHex(@Nonnull String str) {
    str = StringUtil.trimStart(str, "#");
    if (str.length() == 3) {
      return new RGBColor(17 * Integer.valueOf(String.valueOf(str.charAt(0)), 16), 17 * Integer.valueOf(String.valueOf(str.charAt(1)), 16), 17 * Integer.valueOf(String.valueOf(str.charAt(2)), 16));
    }
    else if (str.length() == 6) {
      return RGBColor.decode("0x" + str);
    }
    else {
      throw new IllegalArgumentException("Should be String of 3 or 6 chars length.");
    }
  }

  @Nonnull
  public static String toHtmlColor(@Nonnull final ColorValue c) {
    return "#" + toHex(c);
  }

  public static String toHex(@Nonnull final ColorValue c) {
    RGBColor rgb = c.toRGB();

    final String R = Integer.toHexString(rgb.getRed());
    final String G = Integer.toHexString(rgb.getGreen());
    final String B = Integer.toHexString(rgb.getBlue());
    return (R.length() < 2 ? "0" : "") + R + (G.length() < 2 ? "0" : "") + G + (B.length() < 2 ? "0" : "") + B;
  }

  /**
   * Checks whether color is dark or not based on perceptional luminosity
   * http://stackoverflow.com/questions/596216/formula-to-determine-brightness-of-rgb-color
   *
   * @param c color to check
   * @return dark or not
   */
  public static boolean isDark(@Nonnull final ColorValue c) {
    RGBColor color = c.toRGB();
    // based on perceptional luminosity, see
    return (1 - (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255) >= 0.5;
  }

  private static final double FACTOR = 0.7;

  /**
   * Creates a new {@code Color} that is a brighter version of this
   * {@code Color}.
   * <p>
   * This method applies an arbitrary scale factor to each of the three RGB
   * components of this {@code Color} to create a brighter version
   * of this {@code Color}.
   * The {@code alpha} value is preserved.
   * Although {@code brighter} and
   * {@code darker} are inverse operations, the results of a
   * series of invocations of these two methods might be inconsistent
   * because of rounding errors.
   *
   * @return a new {@code Color} object that is
   * a brighter version of this {@code Color}
   * with the same {@code alpha} value.
   * @see #darker
   */
  public static RGBColor brighter(@Nonnull ColorValue colorValue) {
    RGBColor rgb = colorValue.toRGB();

    int r = rgb.getRed();
    int g = rgb.getGreen();
    int b = rgb.getBlue();
    int alpha = rgb.getAlpha();

    /* From 2D group:
     * 1. black.brighter() should return grey
     * 2. applying brighter to blue will always return blue, brighter
     * 3. non pure color (non zero rgb) will eventually return white
     */
    int i = (int)(1.0 / (1.0 - FACTOR));
    if (r == 0 && g == 0 && b == 0) {
      return new RGBColor(i, i, i, alpha);
    }
    if (r > 0 && r < i) r = i;
    if (g > 0 && g < i) g = i;
    if (b > 0 && b < i) b = i;

    return new RGBColor(Math.min((int)(r / FACTOR), 255), Math.min((int)(g / FACTOR), 255), Math.min((int)(b / FACTOR), 255), alpha);
  }

  /**
   * Creates a new {@code Color} that is a darker version of this
   * {@code Color}.
   * <p>
   * This method applies an arbitrary scale factor to each of the three RGB
   * components of this {@code Color} to create a darker version of
   * this {@code Color}.
   * The {@code alpha} value is preserved.
   * Although {@code brighter} and
   * {@code darker} are inverse operations, the results of a series
   * of invocations of these two methods might be inconsistent because
   * of rounding errors.
   *
   * @return a new {@code Color} object that is
   * a darker version of this {@code Color}
   * with the same {@code alpha} value.
   * @see #brighter
   */
  public static RGBColor darker(@Nonnull ColorValue colorValue) {
    RGBColor rgb = colorValue.toRGB();
    return new RGBColor(Math.max((int)(rgb.getRed() * FACTOR), 0), Math.max((int)(rgb.getGreen() * FACTOR), 0), Math.max((int)(rgb.getBlue() * FACTOR), 0), rgb.getAlpha());
  }

  private static int shift(int colorComponent, double d) {
    final int n = (int)(colorComponent * d);
    return n > 255 ? 255 : n < 0 ? 0 : n;
  }

  public static ColorValue shift(ColorValue c, double d) {
    RGBColor rgb = c.toRGB();
    return new RGBColor(shift(rgb.getRed(), d), shift(rgb.getGreen(), d), shift(rgb.getBlue(), d), rgb.getAlpha());
  }

  @Nonnull
  public static ColorValue mix(@Nonnull ColorValue v1, @Nonnull ColorValue v2, double balance) {
    RGBColor c1 = v1.toRGB();
    RGBColor c2 = v2.toRGB();

    balance = Math.min(1, Math.max(0, balance));
    return new RGBColor((int)((1 - balance) * c1.getRed() + c2.getRed() * balance + .5), (int)((1 - balance) * c1.getGreen() + c2.getGreen() * balance + .5),
                        (int)((1 - balance) * c1.getBlue() + c2.getBlue() * balance + .5), (int)((1 - balance) * c1.getAlpha() + c2.getAlpha() * balance + .5));
  }
}
