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
package consulo.component.internal;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The instance of an extension, set by the extension system right after the extension is created. Until then - on another
 * thread, between the class initialization and the end of the creation - it is looked up like {@link LazyExtensionInstance}.
 *
 * @author VISTALL
 * @since 2023-01-27
 */
public class StableExtensionInstance<Api, Impl extends Api> implements Supplier<Impl> {
  private final Supplier<Impl> myLookup;
  private volatile @Nullable Impl myValue = null;

  public StableExtensionInstance(Class<Api> apiClass, Class<Impl> implClass) {
    myLookup = new LazyExtensionInstance<>(apiClass, implClass);
  }

  public void setValue(Impl value) {
    myValue = value;
  }

  @Override
  public Impl get() {
    Impl value = myValue;
    return value != null ? value : myLookup.get();
  }
}
