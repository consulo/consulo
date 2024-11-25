/*
 * @(#)ColorUtils.java 9/19/2006
 *
 * Copyright 2002 - 2006 JIDE Software Inc. All rights reserved.
 */

package com.jidesoft.utils;

import javax.swing.plaf.ColorUIResource;
import java.awt.*;

/**
 * Several useful methods for Color.
 */
public class ColorUtils {

    /**
     * Gets a derived color from an existing color. The derived color is either lighter or darker version of the given
     * color with the same hue.
     *
     * @param color the given color.
     * @param ratio the ratio. 0.5f if the same color. Any ratio greater than 0.5f will make the result color lighter.
     *              Smaller than 0.5f will make the color darker.
     * @return the derived color.
     */
    public static Color getDerivedColor(Color color, float ratio) {
        if (color != null) {
            float[] hsl = RGBtoHSL(color);
            if (hsl[2] < 0.4) {
                hsl[2] = 0.4f;
            }
            if (ratio > 0.5) {
                hsl[2] += (1f - hsl[2]) * 2 * (ratio - 0.5);
            }
            else {
                hsl[2] -= hsl[2] * 2 * (0.5 - ratio);
            }
            int colorRGB = HSLtoRGB(hsl);
            return new ColorUIResource(colorRGB);
        }
        else {
            return null;
        }
    }

    /**
     * Converts a color from RBG to HSL color space.
     *
     * @param colorRGB the Color.
     * @return color space in HSL.
     */
    public static float[] RGBtoHSL(Color colorRGB) {
        float r, g, b, h, s, l; //this function works with floats between 0 and 1
        r = colorRGB.getRed() / 256.0f;
        g = colorRGB.getGreen() / 256.0f;
        b = colorRGB.getBlue() / 256.0f;

        // Then, minColor and maxColor are defined. Min color is the value of the color component with
        // the smallest value, while maxColor is the value of the color component with the largest value.
        // These two variables are needed because the Lightness is defined as (minColor + maxColor) / 2.

        float maxColor = Math.max(r, Math.max(g, b));
        float minColor = Math.min(r, Math.min(g, b));

        // If minColor equals maxColor, we know that R=G=B and thus the color is a shade of gray.
        // This is a trivial case, hue can be set to anything, saturation has to be set to 0 because
        // only then it's a shade of gray, and lightness is set to R=G=B, the shade of the gray.

        //R == G == B, so it's a shade of gray
        if (r == g && g == b) {
            h = 0.0f; //it doesn't matter what value it has
            s = 0.0f;
            l = r; //doesn't matter if you pick r, g, or b
        }

        // If minColor is not equal to maxColor, we have a real color instead of a shade of gray, so more calculations are needed:

        // Lightness (l) is now set to it's definition of (minColor + maxColor)/2.
        // Saturation (s) is then calculated with a different formula depending if light is in the first half of the second half. This is because the HSL model can be represented as a double cone, the first cone has a black tip and corresponds to the first half of lightness values, the second cone has a white tip and contains the second half of lightness values.
        // Hue (h) is calculated with a different formula depending on which of the 3 color components is the dominating one, and then normalized to a number between 0 and 1.

        else {
            l = (minColor + maxColor) / 2;

            if (l < 0.5) s = (maxColor - minColor) / (maxColor + minColor);
            else s = (maxColor - minColor) / (2.0f - maxColor - minColor);

            if (r == maxColor) h = (g - b) / (maxColor - minColor);
            else if (g == maxColor) h = 2.0f + (b - r) / (maxColor - minColor);
            else h = 4.0f + (r - g) / (maxColor - minColor);

            h /= 6; //to bring it to a number between 0 and 1
            if (h < 0) h++;
        }

        // Finally, H, S and L are calculated out of h, s and l as integers between 0 and 255 and "returned"
        // as the result. Returned, because H, S and L were passed by reference to the function.

        float[] hsl = new float[3];
        hsl[0] = h;
        hsl[1] = s;
        hsl[2] = l;
        return hsl;
    }

    /**
     * Converts from HSL color space to RGB color.
     *
     * @param hsl the hsl values.
     * @return the RGB color.
     */
    public static int HSLtoRGB(float[] hsl) {
        float r, g, b, h, s, l; //this function works with floats between 0 and 1
        float temp1, temp2, tempr, tempg, tempb;
        h = hsl[0];
        s = hsl[1];
        l = hsl[2];

        // Then follows a trivial case: if the saturation is 0, the color will be a grayscale color,
        // and the calculation is then very simple: r, g and b are all set to the lightness.

        //If saturation is 0, the color is a shade of gray
        if (s == 0) {
            r = g = b = l;
        }
        // If the saturation is higher than 0, more calculations are needed again.
        // red, green and blue are calculated with the formulas defined in the code.
        // If saturation > 0, more complex calculations are needed
        else {
            //Set the temporary values
            if (l < 0.5) temp2 = l * (1 + s);
            else temp2 = (l + s) - (l * s);
            temp1 = 2 * l - temp2;
            tempr = h + 1.0f / 3.0f;
            if (tempr > 1) tempr--;
            tempg = h;
            tempb = h - 1.0f / 3.0f;
            if (tempb < 0) tempb++;

            //Red
            if (tempr < 1.0 / 6.0) r = temp1 + (temp2 - temp1) * 6.0f * tempr;
            else if (tempr < 0.5) r = temp2;
            else if (tempr < 2.0 / 3.0) r = temp1 + (temp2 - temp1) * ((2.0f / 3.0f) - tempr) * 6.0f;
            else r = temp1;

            //Green
            if (tempg < 1.0 / 6.0) g = temp1 + (temp2 - temp1) * 6.0f * tempg;
            else if (tempg < 0.5) g = temp2;
            else if (tempg < 2.0 / 3.0) g = temp1 + (temp2 - temp1) * ((2.0f / 3.0f) - tempg) * 6.0f;
            else g = temp1;

            //Blue
            if (tempb < 1.0 / 6.0) b = temp1 + (temp2 - temp1) * 6.0f * tempb;
            else if (tempb < 0.5) b = temp2;
            else if (tempb < 2.0 / 3.0) b = temp1 + (temp2 - temp1) * ((2.0f / 3.0f) - tempb) * 6.0f;
            else b = temp1;
        }

        // And finally, the results are returned as integers between 0 and 255.

        int result = 0;
        result += ((int) (r * 255) & 0xFF) << 16;
        result += ((int) (g * 255) & 0xFF) << 8;
        result += ((int) (b * 255) & 0xFF);

        return result;
    }

    static final float OFFSET_180 = 180f;
    static final float OFFSET_100 = 100f;

    public static int[] calculateDifferent(float[] from, float[] to) {
        int[] diff = new int[3];
        diff[0] = floatToInteger(from[0], to[0], OFFSET_180, true);
        diff[1] = floatToInteger(from[1], to[1], OFFSET_100, false);
        diff[2] = floatToInteger(from[2], to[2], OFFSET_100, false);
        return diff;
    }

    public static float[] applyDifference(float[] from, int[] diff) {
        float[] to = new float[3];
        to[0] = integerToFloat(from[0], diff[0], OFFSET_180, true);
        to[1] = integerToFloat(from[1], diff[1], OFFSET_100, false);
        to[2] = integerToFloat(from[2], diff[2], OFFSET_100, false);
        return to;
    }

    private static int floatToInteger(float f, float f2, float offset, boolean rotate) {
        if (rotate) {
            int i = (int) ((f2 - f) * 2 * offset);
            if (i > offset) {
                return i - (int) (2 * offset);
            }
            else if (i < -offset) {
                return i + (int) (2 * offset);
            }
            else {
                return i;
            }
        }
        else {
            if (f != 0.0f) {
                return (int) ((f2 - f) * offset / f);
            }
            else {
                return (int) ((f2 - f) * offset);
            }
        }
    }

    private static float integerToFloat(float f, int i, float offset, boolean rotate) {
        if (rotate) {
            float v = f + i / (2 * offset);
            if (v < 0.0f) {
                return v + 1.0f;
            }
            else if (v > 1.0f) {
                return v - 1.0f;
            }
            else {
                return v;
            }
        }
        else {
            if (i > 0) {
                return f + (1.0f - f) * i / offset;
            }
            else {
                return f + f * i / offset;
            }
        }
    }

    /**
     * Simply calls new Color(color, hasalpha) for each color in colors and returns all of them.
     *
     * @param hasAlpha true to consider the alpha when creating the Color.
     * @param colors   the color value.
     * @return the colors with alpha added.
     */
    public static Color[] toColors(boolean hasAlpha, int... colors) {
        Color[] result = new Color[colors.length];
        for (int i = 0; i < colors.length; i++) {
            result[i] = new Color(colors[i], hasAlpha);
        }
        return result;
    }

    /**
     * Converts from a color to gray scale color.
     *
     * @param c a color.
     * @return a color in gray scale.
     */
    public static Color toGrayscale(Color c) {
        int gray = (int) (c.getRed() * 0.3 + c.getGreen() * 0.59 + c.getBlue() * 0.11);
        return new Color(gray, gray, gray);
    }
}