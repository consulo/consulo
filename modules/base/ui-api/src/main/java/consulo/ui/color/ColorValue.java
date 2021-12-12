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
package consulo.ui.color;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 12-Oct-17
 */
public interface ColorValue {
  @Nonnull
  static ColorValue lazy(@Nonnull Supplier<? extends ColorValue> getter) {
    return new LazyColorValue(getter);
  }

  @Nonnull
  static ColorValue dummy(@Nonnull String errorMessage) {
    return new ColorValue() {
      @Nonnull
      @Override
      public RGBColor toRGB() {
        throw new UnsupportedOperationException(errorMessage);
      }

      @Nonnull
      @Override
      public ColorValue withAlpha(int value) {
        throw new UnsupportedOperationException(errorMessage);
      }
    };
  }

  @Nonnull
  RGBColor toRGB();

  @Nonnull
  default ColorValue withAlpha(float value) {
    return new WithAlphaColorValue(this, (int)(value * 255 + 0.5f));
  }

  @Nonnull
  default ColorValue withAlpha(int value) {
    return new WithAlphaColorValue(this, value);
  }
}
