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
package consulo.component.extension;

import consulo.component.internal.ExtensionInstanceRef;
import consulo.component.internal.LazyExtensionInstance;
import consulo.component.internal.StableExtensionInstance;

import jakarta.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 26/01/2023
 */
public final class ExtensionInstance {
  @Nonnull
  @SuppressWarnings("unchecked")
  public static <Api, Impl extends Api> Supplier<Impl> from(@Nonnull Class<Api> clazz) {

    // called before extension creating
    ExtensionInstanceRef ref = ExtensionInstanceRef.CURRENT_CREATION.get();
    if (ref == null) {
      Class<Impl> implClazz = (Class<Impl>)StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
      return new LazyExtensionInstance<>(clazz, implClazz);
    }
    else {
      StableExtensionInstance<Impl> instance = new StableExtensionInstance<>();
      ref.setter = o -> instance.setValue((Impl)o);
      return instance;
    }
  }
}
