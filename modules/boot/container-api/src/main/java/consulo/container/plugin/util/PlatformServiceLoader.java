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
package consulo.container.plugin.util;

import consulo.container.StartupError;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * @author VISTALL
 * @since 2019-07-25
 */
public class PlatformServiceLoader {
  @FunctionalInterface
  public interface ServiceLoaderCall {
    <S> ServiceLoader<S> load(Class<S> service, ClassLoader loader);
  }

  /**
   * @param interfaceClass class for service
   * @param loaderCall just method ref for 'ServiceLoader::load' - for selecting grand caller
   */
  public static <T> T findImplementation(Class<T> interfaceClass, ServiceLoaderCall loaderCall) {
    ServiceLoader<T> loader = loaderCall.load(interfaceClass, interfaceClass.getClassLoader());

    Optional<T> first = loader.findFirst();
    if (first.isPresent()) {
      return first.get();
    }

    for (PluginDescriptor descriptor : PluginManager.getPlugins()) {
      if (PluginIds.isPlatformImplementationPlugin(descriptor.getPluginId())) {
        loader = loaderCall.load(interfaceClass, descriptor.getPluginClassLoader());

        first = loader.findFirst();
        if (first.isPresent()) {
          return first.get();
        }
      }
    }

    throw new StartupError("Can't find platform implementation: " + interfaceClass);
  }
}
