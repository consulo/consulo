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
package consulo.extensions;

import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.KeyedLazyInstanceEP;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * Extension extender. It will add additional memebers for select extension. It will be called once when extension cache built.
 * <p>
 * You can't removed those items from extensions later
 *
 * <extensionExtender key='com.intellij.someExtension' implementationClass='SomeExtender' />
 *
 * @author VISTALL
 * @since 2019-02-25
 */
public interface ExtensionExtender<T> {
  ExtensionPointName<KeyedLazyInstanceEP<ExtensionExtender>> EP_NAME = ExtensionPointName.create("com.intellij.extensionExtender");

  void extend(@Nonnull ComponentManager componentManager, @Nonnull Consumer<T> consumer);
}
