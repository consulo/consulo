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
import consulo.configurable.ConfigurableBuilderState;
import consulo.configurable.UnnamedConfigurable;
import consulo.ui.Component;
import consulo.ui.ValueComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 05/03/2023
 */
public class ConfigurableBuilderImpl<Instance extends ConfigurableBuilderState> implements ConfigurableBuilder<Instance> {

  private final Supplier<Instance> myInstanceFactory;
  private final List<Object> myEntries = new ArrayList<>();

  public ConfigurableBuilderImpl(Supplier<Instance> instanceFactory) {
    myInstanceFactory = instanceFactory;
  }

  @Nonnull
  @Override
  public <V, C extends ValueComponent<V>> ConfigurableBuilder<Instance> valueComponent(@Nonnull Supplier<C> valueComponentFactory,
                                                             @Nonnull Supplier<V> getter,
                                                             @Nonnull Consumer<V> setter,
                                                             @Nonnull BiConsumer<Instance, C> instanceSetter) {
    myEntries.add(new ValueComponentProperty(valueComponentFactory, getter, setter, instanceSetter));
    return this;
  }

  @Nonnull
  @Override
  public ConfigurableBuilder<Instance> component(@Nonnull Supplier<Component> component) {
    myEntries.add(component);
    return this;
  }

  @Nonnull
  @Override
  public UnnamedConfigurable buildUnnamed() {
    return new BuilderSimpleConfigurableByProperties<>(myInstanceFactory, List.copyOf(myEntries));
  }
}
