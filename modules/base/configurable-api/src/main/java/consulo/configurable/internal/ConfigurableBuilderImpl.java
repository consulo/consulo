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
package consulo.configurable.internal;

import consulo.configurable.ConfigurableBuilder;
import consulo.configurable.UnnamedConfigurable;
import consulo.ui.ValueComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 05/03/2023
 */
public class ConfigurableBuilderImpl implements ConfigurableBuilder {

  private final List<ValueComponentProperty> myProperties = new ArrayList<>();

  @Nonnull
  @Override
  public <V> ConfigurableBuilder valueComponent(@Nonnull Supplier<ValueComponent<V>> valueComponentFactory,
                                                @Nonnull Supplier<V> getter,
                                                @Nonnull Consumer<V> setter) {
    myProperties.add(new ValueComponentProperty(valueComponentFactory, getter, setter));
    return this;
  }

  @Nonnull
  @Override
  public UnnamedConfigurable buildUnnamed() {
    return new BuilderSimpleConfigurableByProperties(List.copyOf(myProperties));
  }
}
