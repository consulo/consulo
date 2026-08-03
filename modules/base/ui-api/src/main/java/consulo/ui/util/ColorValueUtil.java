package consulo.ui.util;

import consulo.annotation.UsedInPlugin;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

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
     * <code>#FFF</code>,
     * and the same forms with a trailing alpha digit - <code>#FFF8</code>, <code>#abc12380</code>.
     *
     * @param str hex string
     * @return RGBColor object
     */
    public static RGBColor fromHex(String str) {
        RGBColor color = fromHexOrNull(str);
        if (color == null) {
            throw new IllegalArgumentException("Should be String of 3, 4, 6 or 8 chars length.");
        }
        return color;
    }

    public static @Nullable RGBColor fromHexOrNull(@Nullable String str) {
        if (str == null) {
            return null;
        }

        String hex = StringUtil.trimStart(str, "#");
        return switch (hex.length()) {
            case 3 -> new RGBColor(fromHex1(hex, 0), fromHex1(hex, 1), fromHex1(hex, 2), 255);
            case 4 -> new RGBColor(fromHex1(hex, 0), fromHex1(hex, 1), fromHex1(hex, 2), fromHex1(hex, 3));
            case 6 -> new RGBColor(fromHex2(hex, 0), fromHex2(hex, 2), fromHex2(hex, 4), 255);
            case 8 -> new RGBColor(fromHex2(hex, 0), fromHex2(hex, 2), fromHex2(hex, 4), fromHex2(hex, 6));
            default -> null;
        };
    }

    private static int fromHexDigit(String str, int pos) {
        char ch = str.charAt(pos);
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }
        if (ch >= 'A' && ch <= 'F') {
            return ch - 'A' + 10;
        }
        if (ch >= 'a' && ch <= 'f') {
            return ch - 'a' + 10;
        }
        throw new IllegalArgumentException("unsupported char at " + pos + ":" + str);
    }

    private static int fromHex1(String str, int pos) {
        return 17 * fromHexDigit(str, pos);
    }

    private static int fromHex2(String str, int pos) {
        return 16 * fromHexDigit(str, pos) + fromHexDigit(str, pos + 1);
    }

    public static String toHtmlColor(ColorValue c) {
        return "#" + toHex(c);
    }

    public static String toHex(ColorValue c) {
        RGBColor rgb = c.toRGB();

        String R = Integer.toHexString(rgb.getRed());
        String G = Integer.toHexString(rgb.getGreen());
        String B = Integer.toHexString(rgb.getBlue());
        return (R.length() < 2 ? "0" : "") + R + (G.length() < 2 ? "0" : "") + G + (B.length() < 2 ? "0" : "") + B;
    }

    /**
     * Checks whether color is dark or not based on perceptional luminosity
     * http://stackoverflow.com/questions/596216/formula-to-determine-brightness-of-rgb-color
     *
     * @param c color to check
     * @return dark or not
     */
    public static boolean isDark(ColorValue c) {
        RGBColor color = c.toRGB();
        // based on perceptional luminosity, see
        return (1 - (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255) >= 0.5;
    }

    private static final double FACTOR = 0.7;

    /**
     * Creates a new {@code Color} that is a brighter version of this {@code Color}.
     * <p>
     * This method applies an arbitrary scale factor to each of the three RGB
     * components of this {@code Color} to create a brighter version of this {@code Color}.
     * The {@code alpha} value is preserved.
     * Although {@code brighter} and {@code darker} are inverse operations, the results of a
     * series of invocations of these two methods might be inconsistent because of rounding errors.
     *
     * @return a new {@code Color} object that is a brighter version of this {@code Color} with the same {@code alpha} value.
     * @see #darker
     */
    public static RGBColor brighter(ColorValue colorValue) {
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
        int i = (int) (1.0 / (1.0 - FACTOR));
        if (r == 0 && g == 0 && b == 0) {
            return new RGBColor(i, i, i, alpha);
        }
        if (r > 0 && r < i) {
            r = i;
        }
        if (g > 0 && g < i) {
            g = i;
        }
        if (b > 0 && b < i) {
            b = i;
        }

        return new RGBColor(Math.min((int) (r / FACTOR), 255), Math.min((int) (g / FACTOR), 255), Math.min((int) (b / FACTOR), 255), alpha);
    }

    /**
     * Creates a new {@code Color} that is a darker version of this {@code Color}.
     * <p>
     * This method applies an arbitrary scale factor to each of the three RGB
     * components of this {@code Color} to create a darker version of this {@code Color}.
     * The {@code alpha} value is preserved.
     * Although {@code brighter} and {@code darker} are inverse operations, the results of a series
     * of invocations of these two methods might be inconsistent because of rounding errors.
     *
     * @return a new {@code Color} object that is a darker version of this {@code Color} with the same {@code alpha} value.
     * @see #brighter
     */
    public static RGBColor darker(ColorValue colorValue) {
        RGBColor rgb = colorValue.toRGB();
        return new RGBColor(
            Math.max((int) (rgb.getRed() * FACTOR), 0),
            Math.max((int) (rgb.getGreen() * FACTOR), 0),
            Math.max((int) (rgb.getBlue() * FACTOR), 0),
            rgb.getAlpha()
        );
    }

    private static int shift(int colorComponent, double d) {
        int n = (int) (colorComponent * d);
        return n > 255 ? 255 : n < 0 ? 0 : n;
    }

    public static ColorValue shift(ColorValue c, double d) {
        RGBColor rgb = c.toRGB();
        return new RGBColor(shift(rgb.getRed(), d), shift(rgb.getGreen(), d), shift(rgb.getBlue(), d), rgb.getAlpha());
    }

    public static ColorValue mix(ColorValue v1, ColorValue v2, double balance) {
        RGBColor c1 = v1.toRGB();
        RGBColor c2 = v2.toRGB();

        balance = Math.min(1, Math.max(0, balance));
        return new RGBColor(
            (int) ((1 - balance) * c1.getRed() + c2.getRed() * balance + .5),
            (int) ((1 - balance) * c1.getGreen() + c2.getGreen() * balance + .5),
            (int) ((1 - balance) * c1.getBlue() + c2.getBlue() * balance + .5),
            (int) ((1 - balance) * c1.getAlpha() + c2.getAlpha() * balance + .5)
        );
    }
}
