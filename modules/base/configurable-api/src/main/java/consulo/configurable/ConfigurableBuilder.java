/*
 * Copyright 2013-2023 consulo.io
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
package consulo.configurable;

import consulo.configurable.internal.ConfigurableBuilderImpl;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 05/03/2023
 */
public interface ConfigurableBuilder {
  @Nonnull
  static ConfigurableBuilder newBuilder() {
    return new ConfigurableBuilderImpl();
  }

  @Nonnull
  default ConfigurableBuilder checkBox(@Nonnull LocalizeValue label, @Nonnull Supplier<Boolean> getter, @Nonnull Consumer<Boolean> setter) {
    return valueComponent(() -> CheckBox.create(label), getter, setter);
  }

  @Nonnull
  <V> ConfigurableBuilder valueComponent(@Nonnull @RequiredUIAccess Supplier<ValueComponent<V>> valueComponentFactory,
                                         @Nonnull Supplier<V> getter,
                                         @Nonnull Consumer<V> setter);

  @Nonnull
  UnnamedConfigurable buildUnnamed();
}
