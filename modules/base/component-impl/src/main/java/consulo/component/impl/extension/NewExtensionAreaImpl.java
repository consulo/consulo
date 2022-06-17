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
package consulo.component.impl.extension;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.component.ComponentManager;
import consulo.component.bind.InjectingBinding;
import consulo.component.extension.ExtensionPoint;
import consulo.component.internal.InjectingBindingHolder;
import consulo.component.internal.InjectingBindingLoader;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 17-Jun-22
 */
public class NewExtensionAreaImpl {
  private final Map<String, NewExtensionPointImpl> myExtensionPoints = new ConcurrentHashMap<>();

  private final ComponentManager myComponentManager;
  private final ComponentScope myComponentScope;
  private final Runnable myCheckCanceled;
  private final InjectingBindingLoader myInjectingBindingLoader;

  public NewExtensionAreaImpl(ComponentManager componentManager, ComponentScope componentScope, Runnable checkCanceled) {
    myComponentManager = componentManager;
    myComponentScope = componentScope;
    myCheckCanceled = checkCanceled;
    myInjectingBindingLoader = InjectingBindingLoader.INSTANCE; // TODO ref it
  }

  public void registerFromInjectingBinding() {
    InjectingBindingHolder holder = myInjectingBindingLoader.getHolder(Extension.class, myComponentScope);

    for (Map.Entry<String, List<InjectingBinding>> entry : holder.getBindings().entrySet()) {
      myExtensionPoints.put(entry.getKey(), new NewExtensionPointImpl(entry.getKey(), entry.getValue(), myComponentManager));
    }
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public <T> ExtensionPoint<T> getExtensionPoint(@Nonnull Class<T> extensionClass) {
    NewExtensionPointImpl point = myExtensionPoints.get(extensionClass.getName());
    if (point != null) {
      point.initialize(extensionClass);
      return point;
    }

    Extension annotation = extensionClass.getAnnotation(Extension.class);
    if (annotation == null) {
      throw new UnsupportedOperationException(extensionClass + " is not annotated by @Extension");
    }

    ComponentScope value = annotation.value();
    if (value != myComponentScope) {
      throw new UnsupportedOperationException("Wrong extension scope " + value + " vs " + myComponentScope);
    }

    return new NewEmptyExtensionPoint<>(extensionClass);
  }
}
