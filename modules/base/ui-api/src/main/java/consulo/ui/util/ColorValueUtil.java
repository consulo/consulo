package consulo.ui.util;

import com.uber.nullaway.annotations.Contract;
import consulo.annotation.UsedInPlugin;
import consulo.ui.color.ColorValue;
import consulo.ui.color.HSLColor;
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

    @Contract("null -> null")
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
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        if ('A' <= ch && ch <= 'F') {
            return ch - 'A' + 10;
        }
        if ('a' <= ch && ch <= 'f') {
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

    @Contract("null -> null; !null -> !null")
    public static @Nullable String toCssColor(@Nullable ColorValue c) {
        if (c == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();

        if (c instanceof HSLColor hsl) {
            int alpha = hsl.getAlpha();

            result.append(alpha == 255 ? "hsl(" : "hsla(");
            result.append(Math.round(hsl.getHue())).append("deg");
            appendPercent(hsl.getSaturation(), result.append(", "));
            appendPercent(hsl.getLightness(), result.append(", "));
            if (alpha != 255) {
                appendPercent(alpha / 255f, result.append(", "));
            }
            return result.append(')').toString();
        }

        RGBColor rgb = c.toRGB();

        int alpha = rgb.getAlpha();
        if (alpha != 255) {
            result.append("rgba(")
                .append(rgb.getRed()).append(", ")
                .append(rgb.getGreen()).append(", ")
                .append(rgb.getBlue()).append(", ");

            appendPercent(alpha / 255f, result);

            return result.append(')').toString();
        }

        return appendHex(rgb, true, result.append('#')).toString();
    }

    private static StringBuilder appendPercent(float value, StringBuilder sb) {
        int scaled = Math.round(value * 1000);
        int whole = scaled / 10;
        int frac = scaled % 10;
        sb.append(whole);
        if (frac != 0) {
            sb.append('.').append(frac);
        }
        return sb.append('%');
    }

    public static String toHex(ColorValue c) {
        return appendHex(c.toRGB(), false, new StringBuilder(6)).toString();
    }

    private static StringBuilder appendHex(RGBColor rgb, boolean canCollapseTo3, StringBuilder sb) {
        int r = rgb.getRed();
        int g = rgb.getGreen();
        int b = rgb.getBlue();

        char r1 = toHexDigit((r >> 4) & 0xF);
        char r0 = toHexDigit(r & 0xF);
        char g1 = toHexDigit((g >> 4) & 0xF);
        char g0 = toHexDigit(g & 0xF);
        char b1 = toHexDigit((b >> 4) & 0xF);
        char b0 = toHexDigit(b & 0xF);

        if (canCollapseTo3 && r0 == r1 && g0 == g1 && b0 == b1) {
            sb.ensureCapacity(sb.length() + 3);
            return sb.append(r0).append(g0).append(b0);
        }

        sb.ensureCapacity(sb.length() + 6);
        return sb.append(r1).append(r0).append(g1).append(g0).append(b1).append(b0);
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    private static char toHexDigit(int value) {
        return HEX_DIGITS[value & 0xF];
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
        return 0.299f * color.getRed() + 0.587f * color.getGreen() + 0.114f * color.getBlue() < 127.5f;
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
