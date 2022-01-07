/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui.decorator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2018-07-23
 * <p>
 * FIXME [VISTALL] move it to desktop module
 */
public interface SwingUIDecorator extends UIDecorator {
  static <ARG> void apply(BiPredicate<SwingUIDecorator, ARG> predicate, ARG arg) {
    UIDecorator.apply(predicate, arg, SwingUIDecorator.class);
  }

  @Nonnull
  static <R> R get(@Nonnull Function<SwingUIDecorator, R> supplier) {
    return UIDecorator.get(supplier, SwingUIDecorator.class);
  }

  @Nonnull
  static <R> R get(@Nonnull Function<SwingUIDecorator, R> supplier, @Nonnull R defaultValue) {
    return UIDecorator.get(supplier, SwingUIDecorator.class, defaultValue);
  }

  @Nullable
  default Color getSidebarColor() {
    return null;
  }

  default boolean decorateToolbarComboBox(@Nonnull JComboBox<?> comboBox) {
    return false;
  }

  default boolean decorateWindowTitle(@Nonnull JRootPane rootPane) {
    return false;
  }

  default boolean decorateSidebarTree(@Nonnull JTree tree) {
    return false;
  }

  default boolean decorateHelpButton() {
    return true;
  }
}
