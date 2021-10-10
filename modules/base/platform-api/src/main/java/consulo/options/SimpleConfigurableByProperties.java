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
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.Component;
import consulo.ui.ValueComponent;
import javax.annotation.Nonnull;

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
    private final Component myComponent;

    public LayoutWrapper(Component component) {
      myComponent = component;
    }

    @Nonnull
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

  public static class PropertyBuilder {
    private List<Property<?>> myProperties = new ArrayList<>();

    public <T> void add(@Nonnull ValueComponent<T> valueComponent, @Nonnull Supplier<T> getter, @Nonnull Consumer<T> setter) {
      add(valueComponent::getValue, valueComponent::setValue, getter, setter);
    }

    public <T> void add(@Nonnull Supplier<T> uiGetter, @RequiredUIAccess @Nonnull Consumer<T> uiSetter, @Nonnull Supplier<T> getter, @Nonnull Consumer<T> setter) {
      myProperties.add(new Property<>(uiGetter, uiSetter, getter, setter));
    }
  }

  private List<Property<?>> myProperties = Collections.emptyList();

  @RequiredUIAccess
  @Nonnull
  protected abstract Component createLayout(@Nonnull PropertyBuilder propertyBuilder, @Nonnull Disposable uiDisposabl);

  @RequiredUIAccess
  @Nonnull
  @Override
  protected final SimpleConfigurableByProperties.LayoutWrapper createPanel(@Nonnull Disposable uiDisposable) {
    PropertyBuilder builder;
    Component panel = createLayout(builder = new PropertyBuilder(), uiDisposable);
    myProperties = builder.myProperties;
    return new LayoutWrapper(panel);
  }

  @RequiredUIAccess
  @Override
  protected boolean isModified(@Nonnull SimpleConfigurableByProperties.LayoutWrapper component) {
    for (Property<?> property : myProperties) {
      if (property.isModified()) {
        return true;
      }
    }
    return false;
  }

  @RequiredUIAccess
  @Override
  protected void apply(@Nonnull SimpleConfigurableByProperties.LayoutWrapper component) throws ConfigurationException {
    for (Property<?> property : myProperties) {
      property.apply();
    }
  }

  @RequiredUIAccess
  @Override
  protected void reset(@Nonnull SimpleConfigurableByProperties.LayoutWrapper component) {
    for (Property<?> property : myProperties) {
      property.reset();
    }
  }

  @RequiredUIAccess
  @Override
  protected void disposeUIResources(@Nonnull SimpleConfigurableByProperties.LayoutWrapper component) {
    super.disposeUIResources(component);
    myProperties = Collections.emptyList();
  }
}
