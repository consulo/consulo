/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ui.ex.awt.util;

import consulo.ui.ex.Colored;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.lang.annotation.Annotation;

/**
 * @author max
 * @author Konstantin Bulenkov
 */
public class ColorUtil {
  private ColorUtil() {
  }

  public static Color softer(@Nonnull Color color) {
    if (color.getBlue() > 220 && color.getRed() > 220 && color.getGreen() > 220) return color;
    final float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    return Color.getHSBColor(hsb[0], 0.6f * hsb[1], hsb[2]);
  }

  public static Color darker(@Nonnull Color color, int tones) {
    final float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    float brightness = hsb[2];
    for (int i = 0; i < tones; i++) {
      brightness = Math.max(0, brightness / 1.1F);
      if (brightness == 0) break;
    }
    return Color.getHSBColor(hsb[0], hsb[1], brightness);
  }

  public static Color brighter(@Nonnull Color color, int tones) {
    final float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    float brightness = hsb[2];
    for (int i = 0; i < tones; i++) {
      brightness = Math.min(1, brightness * 1.1F);
      if (brightness == 1) break;
    }
    return Color.getHSBColor(hsb[0], hsb[1], brightness);
  }

  @Nonnull
  public static Color saturate(@Nonnull Color color, int tones) {
    final float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    float saturation = hsb[1];
    for (int i = 0; i < tones; i++) {
      saturation = Math.min(1, saturation * 1.1F);
      if (saturation == 1) break;
    }
    return Color.getHSBColor(hsb[0], saturation, hsb[2]);
  }

  @Nonnull
  public static Color desaturate(@Nonnull Color color, int tones) {
    final float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    float saturation = hsb[1];
    for (int i = 0; i < tones; i++) {
      saturation = Math.max(0, saturation / 1.1F);
      if (saturation == 0) break;
    }
    return Color.getHSBColor(hsb[0], saturation, hsb[2]);
  }

  public static Color dimmer(@Nonnull Color color) {
    float[] rgb = color.getRGBColorComponents(null);

    float alpha = 0.80f;
    float rem = 1 - alpha;
    return new Color(rgb[0] * alpha + rem, rgb[1] * alpha + rem, rgb[2] * alpha + rem);
  }

  private static int shift(int colorComponent, double d) {
    final int n = (int)(colorComponent * d);
    return n > 255 ? 255 : n < 0 ? 0 : n;
  }

  public static Color shift(Color c, double d) {
    return new Color(shift(c.getRed(), d), shift(c.getGreen(), d), shift(c.getBlue(), d), c.getAlpha());
  }

  public static Color withAlpha(Color c, double a) {
    return toAlpha(c, (int)(255 * a));
  }

  @Nonnull
  public static Color withPreAlpha(@Nonnull Color c, double a) {
    float[] rgba = new float[4];

    rgba = withAlpha(c, a).getRGBComponents(rgba);
    return new Color(rgba[0] * rgba[3], rgba[1] * rgba[3], rgba[2] * rgba[3], 1.0f);
  }

  public static Color toAlpha(Color color, int a) {
    Color c = color != null ? color : Color.black;
    return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
  }

  public static Color withAlphaAdjustingDarkness(Color c, double d) {
    return shift(withAlpha(c, d), d);
  }

  @Nonnull
  public static String toHtmlColor(@Nonnull final Color c) {
    return "#" + toHex(c);
  }

  public static String toHex(@Nonnull final Color c) {
    final String R = Integer.toHexString(c.getRed());
    final String G = Integer.toHexString(c.getGreen());
    final String B = Integer.toHexString(c.getBlue());
    return (R.length() < 2 ? "0" : "") + R + (G.length() < 2 ? "0" : "") + G + (B.length() < 2 ? "0" : "") + B;
  }

  /**
   * Return Color object from string. The following formats are allowed:
   * <code>#abc123</code>,
   * <code>ABC123</code>,
   * <code>ab5</code>,
   * <code>#FFF</code>.
   *
   * @param str hex string
   * @return Color object
   */
  public static Color fromHex(String str) {
    str = StringUtil.trimStart(str, "#");
    if (str.length() == 3) {
      return new Color(
              17 * Integer.valueOf(String.valueOf(str.charAt(0)), 16).intValue(),
              17 * Integer.valueOf(String.valueOf(str.charAt(1)), 16).intValue(),
              17 * Integer.valueOf(String.valueOf(str.charAt(2)), 16).intValue());
    }
    else if (str.length() == 6) {
      return Color.decode("0x" + str);
    }
    else {
      throw new IllegalArgumentException("Should be String of 3 or 6 chars length.");
    }
  }

  @Nullable
  public static Color fromHex(String str, @Nullable Color defaultValue) {
    try {
      return fromHex(str);
    }
    catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * Checks whether color is dark or not based on perceptional luminosity
   * http://stackoverflow.com/questions/596216/formula-to-determine-brightness-of-rgb-color
   *
   * @param c color to check
   * @return dark or not
   */
  public static boolean isDark(@Nonnull final Color c) {
    // based on perceptional luminosity, see
    return (1 - (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255) >= 0.5;
  }

  @Nonnull
  public static Color mix(@Nonnull Color c1, @Nonnull Color c2, double balance) {
    balance = Math.min(1, Math.max(0, balance));
    return new Color((int)((1 - balance) * c1.getRed() + c2.getRed() * balance + .5),
                     (int)((1 - balance) * c1.getGreen() + c2.getGreen() * balance + .5),
                     (int)((1 - balance) * c1.getBlue() + c2.getBlue() * balance + .5),
                     (int)((1 - balance) * c1.getAlpha() + c2.getAlpha() * balance + .5));
  }
}
