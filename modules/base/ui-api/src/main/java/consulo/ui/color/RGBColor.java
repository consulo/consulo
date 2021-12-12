/*
 * Copyright 2013-2016 consulo.io
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

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * @author VISTALL
 * @since 21-Jun-16
 */
public final class RGBColor implements Serializable, ColorValue {
  @Nonnull
  public static RGBColor fromFloatValues(float r, float g, float b) {
    return fromFloatValues(r, g, b, 1f);
  }

  @Nonnull
  public static RGBColor fromFloatValues(float r, float g, float b, float a) {
    return new RGBColor((int)(r * 255 + 0.5), (int)(g * 255 + 0.5), (int)(b * 255 + 0.5), (int)(b * 255 + 0.5));
  }

  @Nonnull
  public static RGBColor fromRGBValue(int rgb) {
    return new RGBColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
  }

  public static int toRGBValue(@Nonnull RGBColor rgbColor) {
    int b = rgbColor.getBlue();
    int r = rgbColor.getRed();
    int g = rgbColor.getGreen();
    return /*((a & 0xFF) << 24) | */((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF) << 0);
  }

  /**
   * Converts a <code>String</code> to an integer and returns the
   * specified opaque <code>Color</code>. This method handles string
   * formats that are used to represent octal and hexadecimal numbers.
   *
   * @param nm a <code>String</code> that represents
   *           an opaque color as a 24-bit integer
   * @return the new <code>Color</code> object.
   * @throws NumberFormatException if the specified string cannot
   *                               be interpreted as a decimal,
   *                               octal, or hexadecimal integer.
   * @see Integer#decode
   */
  @Nonnull
  public static RGBColor decode(@Nonnull String nm) {
    int i = Integer.decode(nm);
    return fromRGBValue(i);
  }

  private int myRed;
  private int myGreen;
  private int myBlue;
  private int myAlpha;

  private RGBColor() {
  }

  public RGBColor(int red, int green, int blue, float alpha) {
    this(red, green, blue, Math.round(alpha * 255));
  }

  public RGBColor(int red, int green, int blue) {
    this(red, green, blue, 255);
  }

  public RGBColor(RGBColor color, int alpha) {
    this(color.getRed(), color.getGreen(), color.getBlue(), alpha);
  }

  public RGBColor(int red, int green, int blue, int alpha) {
    myRed = red;
    myGreen = green;
    myBlue = blue;
    myAlpha = alpha;
  }

  public float[] getFloatValues() {
    float[] values = new float[4];
    values[0] = ((float)getRed()) / 255f;
    values[1] = ((float)getGreen()) / 255f;
    values[2] = ((float)getBlue()) / 255f;
    values[3] = ((float)getAlpha()) / 255f;
    return values;
  }

  @Nonnull
  @Override
  public RGBColor toRGB() {
    return this;
  }

  public int getRed() {
    return myRed;
  }

  public int getGreen() {
    return myGreen;
  }

  public int getBlue() {
    return myBlue;
  }

  public int getAlpha() {
    return myAlpha;
  }

  @Nonnull
  @Override
  public ColorValue withAlpha(int value) {
    return new RGBColor(getRed(), getGreen(), getBlue(), value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RGBColor rgbColor = (RGBColor)o;

    if (myRed != rgbColor.myRed) return false;
    if (myGreen != rgbColor.myGreen) return false;
    if (myBlue != rgbColor.myBlue) return false;
    if (Float.compare(rgbColor.myAlpha, myAlpha) != 0) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myRed;
    result = 31 * result + myGreen;
    result = 31 * result + myBlue;
    result = 31 * result + (myAlpha != +0.0f ? Float.floatToIntBits(myAlpha) : 0);
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("RGBColor{");
    sb.append("myRed=").append(myRed);
    sb.append(", myGreed=").append(myGreen);
    sb.append(", myBlue=").append(myBlue);
    sb.append(", myAlpha=").append(myAlpha);
    sb.append('}');
    return sb.toString();
  }
}
