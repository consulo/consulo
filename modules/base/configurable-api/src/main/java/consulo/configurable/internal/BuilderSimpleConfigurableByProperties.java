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

import consulo.configurable.SimpleConfigurableByProperties;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author VISTALL
 * @since 05/03/2023
 */
public class BuilderSimpleConfigurableByProperties extends SimpleConfigurableByProperties {
  private final List<ValueComponentProperty> myValueComponentProperties;

  public BuilderSimpleConfigurableByProperties(List<ValueComponentProperty> valueComponentProperties) {
    myValueComponentProperties = valueComponentProperties;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  protected Component createLayout(@Nonnull PropertyBuilder propertyBuilder,
                                   @Nonnull Disposable uiDisposable) {
    VerticalLayout layout = VerticalLayout.create();
    for (ValueComponentProperty property : myValueComponentProperties) {
      ValueComponent component = property.factory().get();
      layout.add(component);
      propertyBuilder.add(component, property.getter(), property.setter());
    }
    return layout;
  }
}
