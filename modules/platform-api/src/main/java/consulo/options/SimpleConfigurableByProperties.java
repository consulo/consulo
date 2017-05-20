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
package consulo.options;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.NotNullComputable;
import consulo.annotations.RequiredDispatchThread;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.ValueComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 19-Nov-16.
 */
public abstract class SimpleConfigurableByProperties extends SimpleConfigurable<SimpleConfigurableByProperties.LayoutWrapper> {
  protected static class LayoutWrapper implements NotNullComputable<Component> {
    private Component myComponent;

    public LayoutWrapper(Component component) {
      myComponent = component;
    }

    @NotNull
    @Override
    public Component compute() {
      return myComponent;
    }
  }

  private static class Property<T> {
    private Supplier<T> myUIGetter;
    private Consumer<T> myUISetter;
    private Supplier<T> mySettingGetter;
    private Consumer<T> mySettingSetter;

    private Property(Supplier<T> uiGetter, Consumer<T> uiSetter, Supplier<T> settingGetter, Consumer<T> settingSetter) {
      myUIGetter = uiGetter;
      myUISetter = uiSetter;
      mySettingGetter = settingGetter;
      mySettingSetter = settingSetter;
    }

    public boolean isModified() {
      T uiValue = myUIGetter.get();
      T settingValue = mySettingGetter.get();
      return !Comparing.equal(uiValue, settingValue);
    }

    public void reset() {
      myUISetter.accept(mySettingGetter.get());
    }

    public void apply() {
      mySettingSetter.accept(myUIGetter.get());
    }
  }

  protected static class PropertyBuilder {
    private List<Property<?>> myProperties = new ArrayList<>();

    public <T> void add(@NotNull ValueComponent<T> valueComponent, @NotNull Supplier<T> getter, @NotNull Consumer<T> setter) {
      myProperties.add(new Property<T>(valueComponent::getValue, valueComponent::setValue, getter, setter));
    }
  }

  private List<Property<?>> myProperties = Collections.emptyList();

  @RequiredUIAccess
  @NotNull
  protected abstract Component createLayout(PropertyBuilder propertyBuilder);

  @RequiredUIAccess
  @NotNull
  @Override
  protected final SimpleConfigurableByProperties.LayoutWrapper createPanel() {
    PropertyBuilder builder;
    Component panel = createLayout(builder = new PropertyBuilder());
    myProperties = builder.myProperties;
    return new LayoutWrapper(panel);
  }

  @RequiredUIAccess
  @Override
  protected boolean isModified(@NotNull SimpleConfigurableByProperties.LayoutWrapper component) {
    for (Property<?> property : myProperties) {
      if (property.isModified()) {
        return true;
      }
    }
    return false;
  }

  @RequiredUIAccess
  @Override
  protected void apply(@NotNull SimpleConfigurableByProperties.LayoutWrapper component) throws ConfigurationException {
    for (Property<?> property : myProperties) {
      property.apply();
    }
  }

  @RequiredUIAccess
  @Override
  protected void reset(@NotNull SimpleConfigurableByProperties.LayoutWrapper component) {
    for (Property<?> property : myProperties) {
      property.reset();
    }
  }

  @RequiredDispatchThread
  @Override
  protected void disposeUIResources(@NotNull SimpleConfigurableByProperties.LayoutWrapper component) {
    super.disposeUIResources(component);
    myProperties = Collections.emptyList();
  }
}
