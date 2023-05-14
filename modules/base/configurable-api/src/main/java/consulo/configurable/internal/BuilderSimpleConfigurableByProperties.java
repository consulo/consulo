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

import consulo.configurable.ConfigurableBuilderState;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 05/03/2023
 */
public class BuilderSimpleConfigurableByProperties<Instance extends ConfigurableBuilderState> extends SimpleConfigurableByProperties {
  private final List<Object> myEntries;
  private final Supplier<Instance> myInstanceFactory;

  private Instance myInstance;

  public BuilderSimpleConfigurableByProperties(Supplier<Instance> instanceFactory, List<Object> entries) {
    myEntries = entries;
    myInstanceFactory = instanceFactory;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  protected Component createLayout(@Nonnull PropertyBuilder propertyBuilder,
                                   @Nonnull Disposable uiDisposable) {
    myInstance = myInstanceFactory.get();

    VerticalLayout layout = VerticalLayout.create();
    for (Object entry : myEntries) {
      if (entry instanceof ValueComponentProperty valueComponentProperty) {
        ValueComponent component = valueComponentProperty.factory().get();

        layout.add(component);

        valueComponentProperty.instanceSetter().accept(myInstance, component);

        propertyBuilder.add(component, valueComponentProperty.getter(), valueComponentProperty.setter());
      }
      else if (entry instanceof Supplier componentFactory) {
        layout.add((Component)componentFactory.get());
      }
    }

    myInstance.uiCreated();
    return layout;
  }

  @RequiredUIAccess
  @Override
  protected void disposeUIResources(@Nonnull LayoutWrapper component) {
    myInstance = null;
  }
}
