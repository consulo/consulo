/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ui.color;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;

/**
 * @author VISTALL
 * @since 2026-08-28
 */
public final class HSLColor implements Serializable, ColorValue {
    public static HSLColor fromRGB(RGBColor color) {
        float red = color.getRed() / 255f;
        float green = color.getGreen() / 255f;
        float blue = color.getBlue() / 255f;

        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        float delta = max - min;

        float lightness = (max + min) / 2f;

        float hue = 0f;
        float saturation = 0f;

        if (delta != 0f) {
            saturation = lightness > 0.5f ? delta / (2f - max - min) : delta / (max + min);

            if (max == red) {
                hue = (green - blue) / delta + (green < blue ? 6f : 0f);
            }
            else if (max == green) {
                hue = (blue - red) / delta + 2f;
            }
            else {
                hue = (red - green) / delta + 4f;
            }

            hue *= 60f;
        }

        return new HSLColor(hue, saturation, lightness, color.getAlpha());
    }

    private final float myHue;
    private final float mySaturation;
    private final float myLightness;
    private final int myAlpha;

    public HSLColor(float hue, float saturation, float lightness) {
        this(hue, saturation, lightness, 255);
    }

    public HSLColor(float hue, float saturation, float lightness, int alpha) {
        myHue = hue;
        mySaturation = saturation;
        myLightness = lightness;
        myAlpha = alpha;
    }

    @Override
    public RGBColor toRGB() {
        float chroma = (1f - Math.abs(2f * myLightness - 1f)) * mySaturation;
        float sector = normalizedHue() / 60f;
        float second = chroma * (1f - Math.abs(sector % 2f - 1f));
        float match = myLightness - chroma / 2f;

        float red;
        float green;
        float blue;

        if (sector < 1f) {
            red = chroma;
            green = second;
            blue = 0f;
        }
        else if (sector < 2f) {
            red = second;
            green = chroma;
            blue = 0f;
        }
        else if (sector < 3f) {
            red = 0f;
            green = chroma;
            blue = second;
        }
        else if (sector < 4f) {
            red = 0f;
            green = second;
            blue = chroma;
        }
        else if (sector < 5f) {
            red = second;
            green = 0f;
            blue = chroma;
        }
        else {
            red = chroma;
            green = 0f;
            blue = second;
        }

        return new RGBColor(
            Math.round((red + match) * 255f),
            Math.round((green + match) * 255f),
            Math.round((blue + match) * 255f),
            myAlpha
        );
    }

    @Override
    public HSLColor toHSL() {
        return this;
    }

    public float getHue() {
        return myHue;
    }

    public float getSaturation() {
        return mySaturation;
    }

    public float getLightness() {
        return myLightness;
    }

    public int getAlpha() {
        return myAlpha;
    }

    @Override
    public ColorValue withAlpha(int value) {
        return new HSLColor(myHue, mySaturation, myLightness, value);
    }

    private float normalizedHue() {
        float hue = myHue % 360f;
        return hue < 0f ? hue + 360f : hue;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HSLColor that = (HSLColor) o;

        return Float.compare(that.myHue, myHue) == 0
            && Float.compare(that.mySaturation, mySaturation) == 0
            && Float.compare(that.myLightness, myLightness) == 0
            && myAlpha == that.myAlpha;
    }

    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(myHue);
        result = 31 * result + Float.floatToIntBits(mySaturation);
        result = 31 * result + Float.floatToIntBits(myLightness);
        result = 31 * result + myAlpha;
        return result;
    }

    @Override
    public String toString() {
        return "HSLColor{myHue=" + myHue
            + ", mySaturation=" + mySaturation
            + ", myLightness=" + myLightness
            + ", myAlpha=" + myAlpha
            + '}';
    }
}
