/*
 * Copyright 2013-2022 consulo.io
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
package consulo.component.util;

import consulo.component.ComponentManager;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 19/01/2022
 */
public final class ComponentUtil {
  public static <I, C extends ComponentManager> Function<C, I> createLazyInject(@Nonnull Class<I> instanceClass) {
    return new Function<>() {
      private final Key<I> myKey = Key.create(instanceClass.getName());

      @Override
      public I apply(C c) {
        I instance = c.getUserData(myKey);
        if (instance != null) {
          return instance;
        }

        instance = c.getInstance(instanceClass);
        c.putUserData(myKey, instance);
        return instance;
      }
    };
  }
}
