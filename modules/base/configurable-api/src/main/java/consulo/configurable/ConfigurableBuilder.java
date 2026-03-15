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
import consulo.configurable.internal.EmptyConfigurableBuilderState;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.lang.function.BooleanConsumer;

import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.function.*;

/**
 * @author VISTALL
 * @since 05/03/2023
 */
public interface ConfigurableBuilder<S extends ConfigurableBuilderState> {
  
  static ConfigurableBuilder<ConfigurableBuilderState> newBuilder() {
    return newBuilder(() -> EmptyConfigurableBuilderState.INSTANCE);
  }

  
  static <I extends ConfigurableBuilderState> ConfigurableBuilder<I> newBuilder(Supplier<I> instanceFactory) {
    return new ConfigurableBuilderImpl<>(instanceFactory);
  }

  // region CheckBox
  
  default ConfigurableBuilder<S> checkBox(LocalizeValue label,
                                          BooleanSupplier getter,
                                          BooleanConsumer setter) {
    return checkBox(label, getter, setter, (instance, checkBox) -> {
    });
  }

  
  default ConfigurableBuilder<S> checkBox(LocalizeValue label,
                                          BooleanSupplier getter,
                                          BooleanConsumer setter,
                                          BiConsumer<S, CheckBox> instanceSetter) {
    return valueComponent(() -> CheckBox.create(label), getter::getAsBoolean, setter::accept, instanceSetter);
  }
  // endregion

  // region IntBox
  
  default ConfigurableBuilder<S> intBox(IntSupplier getter,
                                         IntConsumer setter) {
    return intBox(getter, setter, (instance, textBox) -> {
    });
  }

  
  default ConfigurableBuilder<S> intBox(IntSupplier getter,
                                         IntConsumer setter,
                                         BiConsumer<S, IntBox> instanceSetter) {
    return valueComponent(IntBox::create, getter::getAsInt, setter::accept, instanceSetter);
  }

  // endregion

  // region TextBox
  
  default ConfigurableBuilder<S> textBox(Supplier<String> getter,
                                         Consumer<String> setter) {
    return textBox(getter, setter, (instance, textBox) -> {
    });
  }

  
  default ConfigurableBuilder<S> textBox(Supplier<String> getter,
                                         Consumer<String> setter,
                                         BiConsumer<S, TextBox> instanceSetter) {
    return valueComponent(TextBox::create, getter, setter, instanceSetter);
  }

  // endregion

  // region TextBoxWithExpandAction

  
  default ConfigurableBuilder<S> textBoxWithExpandAction(@Nullable Image editButtonImage,
                                                         String dialogTitle,
                                                         Function<String, List<String>> parser,
                                                         Function<List<String>, String> joiner,
                                                         Supplier<String> getter,
                                                         Consumer<String> setter) {
    return textBoxWithExpandAction(editButtonImage, dialogTitle, parser, joiner, getter, setter, (instance, valueComponent) -> {
    });
  }

  
  default ConfigurableBuilder<S> textBoxWithExpandAction(@Nullable Image editButtonImage,
                                                         String dialogTitle,
                                                         Function<String, List<String>> parser,
                                                         Function<List<String>, String> joiner,
                                                         Supplier<String> getter,
                                                         Consumer<String> setter,
                                                         BiConsumer<S, TextBoxWithExpandAction> instanceSetter) {
    return valueComponent(() -> TextBoxWithExpandAction.create(editButtonImage, dialogTitle, parser, joiner),
                          getter,
                          setter,
                          instanceSetter);
  }
  // endregion

  // region ValueComponent
  
  default <V, C extends ValueComponent<V>> ConfigurableBuilder<S> valueComponent(@RequiredUIAccess Supplier<C> valueComponentFactory,
                                                                                 Supplier<V> getter,
                                                                                 Consumer<V> setter) {
    return valueComponent(valueComponentFactory, getter, setter, (instance, valueComponent) -> {
    });
  }

  
  <V, C extends ValueComponent<V>> ConfigurableBuilder<S> valueComponent(@RequiredUIAccess Supplier<C> valueComponentFactory,
                                                                         Supplier<V> getter,
                                                                         Consumer<V> setter,
                                                                         BiConsumer<S, C> instanceSetter);
  // endregion

  
  ConfigurableBuilder<S> component(Supplier<Component> componentFactory);

  
  UnnamedConfigurable buildUnnamed();
}
