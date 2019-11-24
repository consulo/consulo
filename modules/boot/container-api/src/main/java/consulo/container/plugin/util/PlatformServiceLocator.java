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

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author VISTALL
 * @since 2019-07-25
 */
public class PlatformServiceLocator {
  @Nonnull
  public static <T> T findImplementation(@Nonnull Class<T> interfaceClass) {
    for (T value : ServiceLoader.load(interfaceClass, interfaceClass.getClassLoader())) {
      return value;
    }

    for (PluginDescriptor descriptor : PluginManager.getPlugins()) {
      if (PluginIds.isPlatformImplementationPlugin(descriptor.getPluginId())) {
        ServiceLoader<T> loader = ServiceLoader.load(interfaceClass, descriptor.getPluginClassLoader());

        Iterator<T> iterator = loader.iterator();
        if (iterator.hasNext()) {
          return iterator.next();
        }
      }
    }

    throw new StartupError("Can't find platform implementation: " + interfaceClass);
  }
}
