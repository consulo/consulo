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
package consulo.ui;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author VISTALL
 * @since 21-Jun-16
 */
public final class RGBColor implements Serializable, ColorValue {
  private int myRed;
  private int myGreed;
  private int myBlue;
  private float myAlpha;

  private RGBColor() {
  }

  public RGBColor(int red, int greed, int blue) {
    myRed = red;
    myGreed = greed;
    myBlue = blue;
  }

  public RGBColor(int red, int greed, int blue, float alpha) {
    myRed = red;
    myGreed = greed;
    myBlue = blue;
    myAlpha = alpha;
  }

  @NotNull
  @Override
  public RGBColor toRGB() {
    return this;
  }

  public int getRed() {
    return myRed;
  }

  public int getGreed() {
    return myGreed;
  }

  public int getBlue() {
    return myBlue;
  }

  public float getAlpha() {
    return myAlpha;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RGBColor rgbColor = (RGBColor)o;

    if (myRed != rgbColor.myRed) return false;
    if (myGreed != rgbColor.myGreed) return false;
    if (myBlue != rgbColor.myBlue) return false;
    if (Float.compare(rgbColor.myAlpha, myAlpha) != 0) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myRed;
    result = 31 * result + myGreed;
    result = 31 * result + myBlue;
    result = 31 * result + (myAlpha != +0.0f ? Float.floatToIntBits(myAlpha) : 0);
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("RGBColor{");
    sb.append("myRed=").append(myRed);
    sb.append(", myGreed=").append(myGreed);
    sb.append(", myBlue=").append(myBlue);
    sb.append(", myAlpha=").append(myAlpha);
    sb.append('}');
    return sb.toString();
  }
}
