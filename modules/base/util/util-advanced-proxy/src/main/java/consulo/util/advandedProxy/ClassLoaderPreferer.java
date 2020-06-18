/*
 * Copyright 2013-2020 consulo.io
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
package consulo.util.advandedProxy;

import consulo.container.classloader.PluginClassLoader;
import consulo.container.impl.PluginHolderModificator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2020-06-18
 */
public class ClassLoaderPreferer {
  /**
   * @return base classloader based on superclass and interfaces
   */
  @Nonnull
  public static ClassLoader preferClassLoader(@Nullable Class<?> superclass, @Nullable Class<?>... interfaces) {
    int maxIndex = -1;
    ClassLoader bestLoader = null;
    ClassLoader nonPluginLoader = null;
    if (interfaces != null && interfaces.length > 0) {
      for (final Class anInterface : interfaces) {
        final ClassLoader loader = anInterface.getClassLoader();
        if (loader instanceof PluginClassLoader) {
          final int order = PluginHolderModificator.getPluginLoadOrder(((PluginClassLoader)loader).getPluginId());
          if (maxIndex < order) {
            maxIndex = order;
            bestLoader = loader;
          }
        }
        else if (nonPluginLoader == null) {
          nonPluginLoader = loader;
        }
      }
    }
    ClassLoader superLoader = null;
    if (superclass != null) {
      superLoader = superclass.getClassLoader();
      if (superLoader instanceof PluginClassLoader && maxIndex < PluginHolderModificator.getPluginLoadOrder(((PluginClassLoader)superLoader).getPluginId())) {
        return superLoader;
      }
    }
    if (bestLoader != null) return bestLoader;
    if (superLoader == null) {
      return nonPluginLoader == null ? ClassLoaderPreferer.class.getClassLoader() : nonPluginLoader;
    }
    else {
      return superLoader;
    }
  }
}
