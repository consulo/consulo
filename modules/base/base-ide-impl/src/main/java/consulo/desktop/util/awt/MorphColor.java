/*
 * Copyright 2013-2017 consulo.io
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
package consulo.desktop.util.awt;

import com.intellij.util.NotNullProducer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

/**
 * @author VISTALL
 * @since 17-Jun-17
 */
public class MorphColor extends Color {
  private static final UIModificationTracker ourTracker = UIModificationTracker.getInstance();

  @Nonnull
  public static Color of(@Nonnull NotNullProducer<Color> func) {
    Color color = func.produce();
    return new MorphColor(color, ourTracker, func);
  }

  @Nonnull
  public static Color ofWithoutCache(@Nonnull NotNullProducer<Color> func) {
    Color color = func.produce();
    return new MorphColor(color, null, func);
  }

  @Nullable
  private UIModificationTracker myModificationTracker;
  @Nonnull
  private final NotNullProducer<Color> myColorProducer;
  private long myLastModificationCount;

  private Color myLastComputedColor;

  private MorphColor(Color color, @Nullable UIModificationTracker modificationTracker, @Nonnull NotNullProducer<Color> function) {
    super(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    myLastComputedColor = color;
    myModificationTracker = modificationTracker;
    myColorProducer = function;
    myLastModificationCount = modificationTracker == null ? -1 : ourTracker.getModificationCount();
  }

  @Nonnull
  private Color getColor() {
    if (myModificationTracker == null) {
      return myColorProducer.produce();
    }
    long modificationCount = myModificationTracker.getModificationCount();
    if (myLastModificationCount == modificationCount) {
      return myLastComputedColor;
    }

    myLastModificationCount = modificationCount;
    return myLastComputedColor = myColorProducer.produce();
  }

  @Override
  public int getRed() {
    final Color c = getColor();
    return c == this ? super.getRed() : c.getRed();
  }

  @Override
  public int getGreen() {
    final Color c = getColor();
    return c == this ? super.getGreen() : c.getGreen();
  }

  @Override
  public int getBlue() {
    final Color c = getColor();
    return c == this ? super.getBlue() : c.getBlue();
  }

  @Override
  public int getAlpha() {
    final Color c = getColor();
    return c == this ? super.getAlpha() : c.getAlpha();
  }

  @Override
  public int getRGB() {
    final Color c = getColor();
    return c == this ? super.getRGB() : c.getRGB();
  }

  @Override
  public Color brighter() {
    return of(new NotNullProducer<Color>() {
      @Nonnull
      @Override
      public Color produce() {
        return myColorProducer.produce().brighter();
      }
    });
  }

  @Override
  public Color darker() {
    return of(new NotNullProducer<Color>() {
      @Nonnull
      @Override
      public Color produce() {
        return myColorProducer.produce().darker();
      }
    });
  }

  @Override
  public int hashCode() {
    final Color c = getColor();
    return c == this ? super.hashCode() : c.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    final Color c = getColor();
    return c == this ? super.equals(obj) : c.equals(obj);
  }

  @Override
  public String toString() {
    final Color c = getColor();
    return c == this ? super.toString() : c.toString();
  }

  @Override
  public float[] getRGBComponents(float[] compArray) {
    final Color c = getColor();
    return c == this ? super.getRGBComponents(compArray) : c.getRGBComponents(compArray);
  }

  @Override
  public float[] getRGBColorComponents(float[] compArray) {
    final Color c = getColor();
    return c == this ? super.getRGBComponents(compArray) : c.getRGBColorComponents(compArray);
  }

  @Override
  public float[] getComponents(float[] compArray) {
    final Color c = getColor();
    return c == this ? super.getComponents(compArray) : c.getComponents(compArray);
  }

  @Override
  public float[] getColorComponents(float[] compArray) {
    final Color c = getColor();
    return c == this ? super.getColorComponents(compArray) : c.getColorComponents(compArray);
  }

  @Override
  public float[] getComponents(ColorSpace cspace, float[] compArray) {
    final Color c = getColor();
    return c == this ? super.getComponents(cspace, compArray) : c.getComponents(cspace, compArray);
  }

  @Override
  public float[] getColorComponents(ColorSpace cspace, float[] compArray) {
    final Color c = getColor();
    return c == this ? super.getColorComponents(cspace, compArray) : c.getColorComponents(cspace, compArray);
  }

  @Override
  public ColorSpace getColorSpace() {
    final Color c = getColor();
    return c == this ? super.getColorSpace() : c.getColorSpace();
  }

  @Override
  public synchronized PaintContext createContext(ColorModel cm, Rectangle r, Rectangle2D r2d, AffineTransform xform, RenderingHints hints) {
    final Color c = getColor();
    return c == this ? super.createContext(cm, r, r2d, xform, hints) : c.createContext(cm, r, r2d, xform, hints);
  }

  @Override
  public int getTransparency() {
    final Color c = getColor();
    return c == this ? super.getTransparency() : c.getTransparency();
  }
}
