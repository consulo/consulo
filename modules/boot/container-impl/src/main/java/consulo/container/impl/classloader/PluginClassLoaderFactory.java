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
import consulo.container.impl.PluginDescriptorImpl;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2019-11-23
 */
public class PluginClassLoaderFactory {
  @SuppressWarnings("unchecked")
  public static <C extends ClassLoader & PluginClassLoader> C create(Set<PluginId> enabledPluginIds, ClassLoader parent, PluginDescriptor pluginDescriptor) throws IOException {
    PluginDescriptorImpl impl = (PluginDescriptorImpl)pluginDescriptor;
    return (C)new PluginClassLoaderImpl(filesToUrls(impl.getClassPath(enabledPluginIds)), parent, pluginDescriptor);
  }

  @SuppressWarnings("unchecked")
  public static <C extends ClassLoader & PluginClassLoader> C create(Set<PluginId> enabledPluginIds, ClassLoader[] parents, PluginDescriptor pluginDescriptor) throws IOException {
    PluginDescriptorImpl impl = (PluginDescriptorImpl)pluginDescriptor;
    return (C)new PluginClassLoaderImpl(filesToUrls(impl.getClassPath(enabledPluginIds)), parents, pluginDescriptor);
  }

  private static List<URL> filesToUrls(List<File> files) throws IOException {
    List<URL> urls = new ArrayList<>(files.size());

    for (File file : files) {
      urls.add(file.toURI().toURL());
    }
    return urls;
  }
}
