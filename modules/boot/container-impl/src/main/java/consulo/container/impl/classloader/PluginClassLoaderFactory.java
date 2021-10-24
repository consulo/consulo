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
package consulo.container.impl.classloader;

import consulo.container.classloader.PluginClassLoader;
import consulo.container.plugin.PluginDescriptor;

import javax.annotation.Nonnull;
import java.net.URL;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-11-23
 */
public class PluginClassLoaderFactory {
  @SuppressWarnings("unchecked")
  @Nonnull
  public static <C extends ClassLoader & PluginClassLoader> C create(@Nonnull List<URL> urls, @Nonnull ClassLoader parent, PluginDescriptor pluginDescriptor) {
    return (C)new PluginClassLoaderImpl(urls, parent, pluginDescriptor);
  }

  @SuppressWarnings("unchecked")
  @Nonnull
  public static <C extends ClassLoader & PluginClassLoader> C create(@Nonnull List<URL> urls, @Nonnull ClassLoader[] parents, PluginDescriptor pluginDescriptor) {
    return (C)new PluginClassLoaderImpl(urls, parents, pluginDescriptor);
  }
}
