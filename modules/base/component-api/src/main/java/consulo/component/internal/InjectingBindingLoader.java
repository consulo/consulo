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
package consulo.component.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.annotation.component.Service;
import consulo.annotation.component.Topic;
import consulo.component.bind.InjectingBinding;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author VISTALL
 * @since 13-Jun-22
 */
public class InjectingBindingLoader {
  public static final InjectingBindingLoader INSTANCE = new InjectingBindingLoader();

  private final Map<ComponentScope, InjectingBindingHolder> myServices = new HashMap<>();
  private final Map<ComponentScope, InjectingBindingHolder> myExtensions = new HashMap<>();
  private final Map<ComponentScope, InjectingBindingHolder> myTopics = new HashMap<>();

  private InjectingBindingLoader() {
  }

  public void analyzeBindings() {
    Set<Class> processed = new HashSet<>();

    List<PluginDescriptor> pluginDescriptors = PluginManager.getPlugins();
    for (PluginDescriptor pluginDescriptor : pluginDescriptors) {
      if (PluginManager.shouldSkipPlugin(pluginDescriptor)) {
        continue;
      }

      ServiceLoader<InjectingBinding> loader = ServiceLoader.load(InjectingBinding.class, pluginDescriptor.getPluginClassLoader());

      for (InjectingBinding binding : loader) {
        ClassLoader classLoader = binding.getClass().getClassLoader();

        // if we loaded binding by another plugin - stop it, since we it will be called #getParent()
        if (classLoader != pluginDescriptor.getPluginClassLoader()) {
          break;
        }

        if (!processed.add(binding.getClass())) {
          throw new IllegalArgumentException("Duplicate registration of binding: " + binding.getClass());
        }

        getHolder(binding.getComponentAnnotationClass(), binding.getComponentScope()).addBinding(binding);
      }
    }
  }

  @Nonnull
  public InjectingBindingHolder getHolder(@Nonnull Class<?> annotationClass, @Nonnull ComponentScope componentScope) {
    if (annotationClass == Service.class) {
      return myServices.computeIfAbsent(componentScope, c -> new InjectingBindingHolder(annotationClass, componentScope));
    }
    else if (annotationClass == Extension.class) {
      return myExtensions.computeIfAbsent(componentScope, c -> new InjectingBindingHolder(annotationClass, componentScope));
    }
    else if (annotationClass == Topic.class) {
      return myTopics.computeIfAbsent(componentScope, c -> new InjectingBindingHolder(annotationClass, componentScope));
    }

    throw new UnsupportedOperationException("Unknown annotation: " + annotationClass);
  }
}
