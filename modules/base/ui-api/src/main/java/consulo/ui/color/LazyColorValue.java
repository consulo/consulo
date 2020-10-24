/*
 * Copyright 2013-2020 consulo.io
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
 * @since 2020-08-09
 */
class LazyColorValue implements ColorValue {
  private Supplier<? extends ColorValue> myColorSupplier;

  LazyColorValue(Supplier<? extends ColorValue> colorSupplier) {
    myColorSupplier = colorSupplier;
  }

  @Nonnull
  @Override
  public RGBColor toRGB() {
    return myColorSupplier.get().toRGB();
  }
}
