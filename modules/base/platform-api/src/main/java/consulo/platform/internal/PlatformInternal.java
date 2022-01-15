/*
 * Copyright 2013-2017 consulo.io
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
package consulo.platform.internal;

import consulo.container.StartupError;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.platform.Platform;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public abstract class PlatformInternal {
  private static final PlatformInternal ourPlatformInternal = findImplementation(PlatformInternal.class);
  private static final Platform ourCurrentPlatform = ourPlatformInternal.build();

  @Nonnull
  private static <T> T findImplementation(@Nonnull Class<T> interfaceClass) {
    Optional<T> thisClassloader = ServiceLoader.load(interfaceClass).findFirst();
    if(thisClassloader.isPresent()) {
      return thisClassloader.get();
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

  @Nonnull
  public static Platform current() {
    return ourCurrentPlatform;
  }

  @Nonnull
  public abstract Platform build();
}
