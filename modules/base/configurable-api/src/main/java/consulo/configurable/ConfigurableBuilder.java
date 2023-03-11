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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.*;

/**
 * @author VISTALL
 * @since 05/03/2023
 */
public interface ConfigurableBuilder<S extends ConfigurableBuilderState> {
  @Nonnull
  static ConfigurableBuilder<EmptyConfigurableBuilderState> newBuilder() {
    return newBuilder(() -> EmptyConfigurableBuilderState.INSTANCE);
  }

  @Nonnull
  static <I extends ConfigurableBuilderState> ConfigurableBuilder<I> newBuilder(@Nonnull Supplier<I> instanceFactory) {
    return new ConfigurableBuilderImpl<>(instanceFactory);
  }

  // region CheckBox
  @Nonnull
  default ConfigurableBuilder<S> checkBox(@Nonnull LocalizeValue label,
                                          @Nonnull BooleanSupplier getter,
                                          @Nonnull BooleanConsumer setter) {
    return checkBox(label, getter, setter, (instance, checkBox) -> {
    });
  }

  @Nonnull
  default ConfigurableBuilder<S> checkBox(@Nonnull LocalizeValue label,
                                          @Nonnull BooleanSupplier getter,
                                          @Nonnull BooleanConsumer setter,
                                          @Nonnull BiConsumer<S, CheckBox> instanceSetter) {
    return valueComponent(() -> CheckBox.create(label), getter::getAsBoolean, setter::accept, instanceSetter);
  }
  // endregion

  // region TextBox
  @Nonnull
  default ConfigurableBuilder<S> textBox(@Nonnull Supplier<String> getter,
                                         @Nonnull Consumer<String> setter) {
    return textBox(getter, setter, (instance, textBox) -> {
    });
  }

  @Nonnull
  default ConfigurableBuilder<S> textBox(@Nonnull Supplier<String> getter,
                                         @Nonnull Consumer<String> setter,
                                         @Nonnull BiConsumer<S, TextBox> instanceSetter) {
    return valueComponent(TextBox::create, getter, setter, instanceSetter);
  }

  @Nonnull
  default ConfigurableBuilder<S> textBoxWithExpandAction(@Nullable Image editButtonImage,
                                                         @Nonnull String dialogTitle,
                                                         @Nonnull Function<String, List<String>> parser,
                                                         @Nonnull Function<List<String>, String> joiner,
                                                         @Nonnull Supplier<String> getter,
                                                         @Nonnull Consumer<String> setter) {
    return textBoxWithExpandAction(editButtonImage, dialogTitle, parser, joiner, getter, setter, (instance, valueComponent) -> {
    });
  }
  //endregion

  // region TextBoxWithExpandAction
  @Nonnull
  default ConfigurableBuilder<S> textBoxWithExpandAction(@Nullable Image editButtonImage,
                                                         @Nonnull String dialogTitle,
                                                         @Nonnull Function<String, List<String>> parser,
                                                         @Nonnull Function<List<String>, String> joiner,
                                                         @Nonnull Supplier<String> getter,
                                                         @Nonnull Consumer<String> setter,
                                                         @Nonnull BiConsumer<S, TextBoxWithExpandAction> instanceSetter) {
    return valueComponent(() -> TextBoxWithExpandAction.create(editButtonImage, dialogTitle, parser, joiner),
                          getter,
                          setter,
                          instanceSetter);
  }
  // endregion

  // region ValueComponent
  @Nonnull
  default <V, C extends ValueComponent<V>> ConfigurableBuilder<S> valueComponent(@Nonnull @RequiredUIAccess Supplier<C> valueComponentFactory,
                                                                                 @Nonnull Supplier<V> getter,
                                                                                 @Nonnull Consumer<V> setter) {
    return valueComponent(valueComponentFactory, getter, setter, (instance, valueComponent) -> {
    });
  }

  @Nonnull
  <V, C extends ValueComponent<V>> ConfigurableBuilder<S> valueComponent(@Nonnull @RequiredUIAccess Supplier<C> valueComponentFactory,
                                                                         @Nonnull Supplier<V> getter,
                                                                         @Nonnull Consumer<V> setter,
                                                                         @Nonnull BiConsumer<S, C> instanceSetter);
  // endregion

  @Nonnull
  ConfigurableBuilder<S> component(@Nonnull Component component);

  @Nonnull
  UnnamedConfigurable buildUnnamed();
}
