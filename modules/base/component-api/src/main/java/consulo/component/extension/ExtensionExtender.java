/*
 * Copyright 2013-2019 consulo.io
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.component.ComponentManager;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * Extension extender. It will add additional members for select extension. It will be called once when extension cache built.
 * <p>
 * You can't removed those items from extensions later
 *
 * @author VISTALL
 * @since 2019-02-25
 */
// TODO not work
@Extension(ComponentScope.APPLICATION)
public interface ExtensionExtender<T> {
  ExtensionPointName<KeyedLazyInstanceEP<ExtensionExtender>> EP_NAME = ExtensionPointName.create("consulo.extensionExtender");

  void extend(@Nonnull ComponentManager componentManager, @Nonnull Consumer<T> consumer);

  @Nonnull
  Class<T> getExtensionClass();
}
