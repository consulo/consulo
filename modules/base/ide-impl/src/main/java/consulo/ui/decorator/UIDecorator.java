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

import com.intellij.util.ObjectUtil;

import javax.annotation.Nonnull;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2018-07-23
 */
public interface UIDecorator {
  static <ARG, U extends UIDecorator> void apply(BiPredicate<U, ARG> predicate, ARG arg, Class<U> clazz) {
    for (UIDecorator decorator : UIDecorators.getDecorators()) {
      if (!decorator.isAvaliable()) {
        continue;
      }

      U u = ObjectUtil.tryCast(decorator, clazz);
      if (u == null) {
        continue;
      }

      if (predicate.test(u, arg)) {
        break;
      }
    }
  }

  @Nonnull
  static <R, U extends UIDecorator> R get(Function<U, R> supplier, Class<U> clazz) {
    for (UIDecorator decorator : UIDecorators.getDecorators()) {
      if (!decorator.isAvaliable()) {
        continue;
      }

      U u = ObjectUtil.tryCast(decorator, clazz);
      if (u == null) {
        continue;
      }

      R fun = supplier.apply(u);
      if (fun != null) {
        return fun;
      }
    }
    throw new IllegalArgumentException("Null value");
  }

  @Nonnull
  static <R, U extends UIDecorator> R get(Function<U, R> supplier, Class<U> clazz, @Nonnull R defaultValue) {
    for (UIDecorator decorator : UIDecorators.getDecorators()) {
      if (!decorator.isAvaliable()) {
        continue;
      }

      U u = ObjectUtil.tryCast(decorator, clazz);
      if (u == null) {
        continue;
      }

      R fun = supplier.apply(u);
      if (fun != null) {
        return fun;
      }
    }
    return defaultValue;
  }

  boolean isAvaliable();

  default boolean isDark() {
    return false;
  }
}
