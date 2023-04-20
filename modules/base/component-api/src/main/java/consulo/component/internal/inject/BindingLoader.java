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
package consulo.component.internal.inject;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 25/01/2023
 */
public abstract class BindingLoader<B> {
  protected final AtomicBoolean myLocked = new AtomicBoolean();

  public void analyzeBindings() {
    Set<Class> processed = new HashSet<>();

    PluginManager.forEachEnabledPlugin(pluginDescriptor -> {
      ModuleLayer moduleLayer = pluginDescriptor.getModuleLayer();
      // this is optimization, since in legacy mode it's read META-INF and module-info services
      // and when load META-INF service loader will skip class, if it from named module (and we have all named)
      // that why load only from module-info
      if (moduleLayer != null) {
        loadInModuleMode(moduleLayer, pluginDescriptor, processed);
      }
      else {
        loadInLegacyMode(pluginDescriptor, processed);
      }
    });

    myLocked.set(true);
  }

  @Nonnull
  protected abstract Class<B> getBindingClass();

  protected abstract void process(@Nonnull B binding);

  private void loadInLegacyMode(@Nonnull PluginDescriptor pluginDescriptor, @Nonnull Set<Class> processed) {
    ServiceLoader<B> loader = ServiceLoader.load(getBindingClass(), pluginDescriptor.getPluginClassLoader());

    for (B binding : loader) {
      ClassLoader classLoader = binding.getClass().getClassLoader();

      // if we loaded binding by another plugin - stop it, since we it will be called #getParent()
      if (classLoader != pluginDescriptor.getPluginClassLoader()) {
        continue;
      }

      if (!processed.add(binding.getClass())) {
        throw new IllegalArgumentException("Duplicate registration of binding: " + binding.getClass());
      }

      try {
        process(binding);
      }
      catch (Throwable e) {
        // TODO [VISTALL] log may not initialized here
        e.printStackTrace();
      }
    }
  }

  private void loadInModuleMode(@Nonnull ModuleLayer moduleLayer, @Nonnull PluginDescriptor pluginDescriptor, @Nonnull Set<Class> processed) {
    ServiceLoader<B> loader = ServiceLoader.load(moduleLayer, getBindingClass());

    Iterator<ServiceLoader.Provider<B>> iterator = loader.stream().iterator();
    while (iterator.hasNext()) {
      ServiceLoader.Provider<B> provider = iterator.next();

      ClassLoader classLoader = provider.type().getClassLoader();

      // if we loaded binding by another plugin - stop it, since we it will be called #getParent()
      if (classLoader != pluginDescriptor.getPluginClassLoader()) {
        break;
      }

      if (!processed.add(provider.type())) {
        throw new IllegalArgumentException("Duplicate registration of binding: " + provider.type());
      }

      B binding = provider.get();
      try {
        process(binding);
      }
      catch (Throwable e) {
        // TODO [VISTALL] log may not initialized here
        e.printStackTrace();
      }
    }
  }
}
