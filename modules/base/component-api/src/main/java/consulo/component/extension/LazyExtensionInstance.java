// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.component.extension;

import consulo.component.ComponentManager;
import consulo.container.PluginException;
import consulo.container.plugin.PluginDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiConsumer;

/**
 * Use only if you need to cache created instance.
 *
 * @see ExtensionPointName#processWithPluginDescriptor(BiConsumer)
 */
@Deprecated(forRemoval = true)
public abstract class LazyExtensionInstance<T> {
  private volatile T instance;

  protected LazyExtensionInstance() {
  }

  protected LazyExtensionInstance(@Nonnull T instance) {
    this.instance = instance;
  }

  @Nullable
  protected abstract String getImplementationClassName();

  @Nonnull
  public final T getInstance(@Nonnull ComponentManager componentManager, @Nonnull PluginDescriptor pluginDescriptor) {
    T result = instance;
    if (result != null) {
      return result;
    }

    //noinspection SynchronizeOnThis
    synchronized (this) {
      result = instance;
      if (result != null) {
        return result;
      }

      result = createInstance(componentManager, pluginDescriptor);
      instance = result;
    }
    return result;
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public T createInstance(@Nonnull ComponentManager componentManager, @Nonnull PluginDescriptor pluginDescriptor) {
    String className = getImplementationClassName();
    if (className == null) {
      throw new PluginException("implementation class is not specified", pluginDescriptor.getPluginId());
    }

    try {
      Class<T> implClass = (Class<T>)Class.forName(className, true, pluginDescriptor.getPluginClassLoader());
      return componentManager.getInjectingContainer().getUnbindedInstance(implClass);
    }
    catch (ClassNotFoundException e) {
      throw new PluginException(e, pluginDescriptor.getPluginId());
    }
  }
}
