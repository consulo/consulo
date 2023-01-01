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
package consulo.component.internal.inject;

import consulo.annotation.component.*;
import consulo.component.bind.InjectingBinding;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 13-Jun-22
 */
public class InjectingBindingLoader {
  public static final InjectingBindingLoader INSTANCE = new InjectingBindingLoader();

  private final AtomicBoolean myLocked = new AtomicBoolean();

  private final Map<ComponentScope, InjectingBindingHolder> myServices = new HashMap<>();
  private final Map<ComponentScope, InjectingBindingHolder> myExtensions = new HashMap<>();
  private final Map<ComponentScope, InjectingBindingHolder> myTopics = new HashMap<>();
  private final InjectingBindingHolder myActions = new InjectingBindingHolder(myLocked);

  private InjectingBindingLoader() {
  }

  public void analyzeBindings() {
    Set<Class> processed = new HashSet<>();

    List<PluginDescriptor> pluginDescriptors = PluginManager.getPlugins();
    for (PluginDescriptor pluginDescriptor : pluginDescriptors) {
      if (PluginManager.shouldSkipPlugin(pluginDescriptor)) {
        continue;
      }

      ModuleLayer moduleLayer = pluginDescriptor.getModuleLayer();
      ServiceLoader<InjectingBinding> loader;
      // this is optimization, since in legacy mode it's read META-INF and module-info services
      // and when load META-INF service loader will skip class, if it from named module (and we have all named)
      // that why load only from module-info
      if (moduleLayer != null) {
        loadInModuleMode(moduleLayer, pluginDescriptor, processed);
      }
      else {
        loadInLegacyMode(pluginDescriptor, processed);
      }
    }

    myLocked.set(true);
  }

  private void loadInLegacyMode(@Nonnull PluginDescriptor pluginDescriptor, @Nonnull Set<Class> processed) {
    ServiceLoader<InjectingBinding> loader = ServiceLoader.load(InjectingBinding.class, pluginDescriptor.getPluginClassLoader());

    for (InjectingBinding binding : loader) {
      ClassLoader classLoader = binding.getClass().getClassLoader();

      // if we loaded binding by another plugin - stop it, since we it will be called #getParent()
      if (classLoader != pluginDescriptor.getPluginClassLoader()) {
        continue;
      }

      if (!processed.add(binding.getClass())) {
        throw new IllegalArgumentException("Duplicate registration of binding: " + binding.getClass());
      }

      try {
        getHolder(binding.getComponentAnnotationClass(), binding.getComponentScope()).addBinding(binding);
      }
      catch (Error e) {
        // TODO [VISTALL] log may not initialized here
        e.printStackTrace();
      }
    }
  }

  private void loadInModuleMode(@Nonnull ModuleLayer moduleLayer, @Nonnull PluginDescriptor pluginDescriptor, @Nonnull Set<Class> processed) {
    ServiceLoader<InjectingBinding> loader = ServiceLoader.load(moduleLayer, InjectingBinding.class);

    Iterator<ServiceLoader.Provider<InjectingBinding>> iterator = loader.stream().iterator();
    while (iterator.hasNext()) {
      ServiceLoader.Provider<InjectingBinding> provider = iterator.next();

      ClassLoader classLoader = provider.type().getClassLoader();

      // if we loaded binding by another plugin - stop it, since we it will be called #getParent()
      if (classLoader != pluginDescriptor.getPluginClassLoader()) {
        break;
      }

      if (!processed.add(provider.type())) {
        throw new IllegalArgumentException("Duplicate registration of binding: " + provider.type());
      }

      InjectingBinding binding = provider.get();
      try {
        getHolder(binding.getComponentAnnotationClass(), binding.getComponentScope()).addBinding(binding);
      }
      catch (Error e) {
        // TODO [VISTALL] log may not initialized here
        e.printStackTrace();
      }
    }
  }

  @Nonnull
  public InjectingBindingHolder getHolder(@Nonnull Class<?> annotationClass, @Nonnull ComponentScope componentScope) {
    if (annotationClass == ServiceAPI.class) {
      return myServices.computeIfAbsent(componentScope, c -> new InjectingBindingHolder(myLocked));
    }
    else if (annotationClass == ExtensionAPI.class) {
      return myExtensions.computeIfAbsent(componentScope, c -> new InjectingBindingHolder(myLocked));
    }
    else if (annotationClass == TopicAPI.class) {
      return myTopics.computeIfAbsent(componentScope, c -> new InjectingBindingHolder(myLocked));
    }
    else if (annotationClass == ActionAPI.class) {
      return myActions;
    }

    throw new UnsupportedOperationException("Unknown annotation: " + annotationClass);
  }
}
