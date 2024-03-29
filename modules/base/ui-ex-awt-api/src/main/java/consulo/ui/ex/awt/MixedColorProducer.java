// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt;

import consulo.util.lang.Couple;

import jakarta.annotation.Nonnull;
import java.awt.*;
import java.util.function.Supplier;

/**
 * This is a color producer that allows dynamically mix two colors.
 */
public final class MixedColorProducer implements Supplier<Color> {
  private final Couple<Color> couple;
  private double mixer;
  private Color cached;
  private int argb0;
  private int argb1;

  public MixedColorProducer(@Nonnull Color color0, @Nonnull Color color1) {
    couple = Couple.of(color0, color1);
  }

  public MixedColorProducer(@Nonnull Color color0, @Nonnull Color color1, double mixer) {
    this(color0, color1);
    setMixer(mixer);
  }

  public void setMixer(double value) {
    if (Double.isNaN(value) || value < 0 || 1 < value) throw new IllegalArgumentException("mixer[0..1] is " + value);
    if (mixer != value) {
      mixer = value;
      cached = null;
    }
  }

  private void updateFirstARGB() {
    int value = couple.first.getRGB();
    if (argb0 != value) {
      argb0 = value;
      cached = null;
    }
  }

  private void updateSecondARGB() {
    int value = couple.second.getRGB();
    if (argb1 != value) {
      argb1 = value;
      cached = null;
    }
  }

  private int mix(int pos) {
    int value0 = 0xFF & (argb0 >> pos);
    int value1 = 0xFF & (argb1 >> pos);
    if (value0 == value1) return value0;
    return value0 + (int)Math.round(mixer * (value1 - value0));
  }

  @Nonnull
  public Color produce(double mixer) {
    setMixer(mixer);
    return get();
  }

  @Nonnull
  @Override
  public Color get() {
    if (mixer <= 0) return couple.first;
    if (mixer >= 1) return couple.second;
    updateFirstARGB();
    updateSecondARGB();
    if (cached == null) {
      //noinspection UseJBColor
      cached = new Color(mix(16), mix(8), mix(0), mix(24));
    }
    return cached;
  }
}
