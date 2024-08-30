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
import consulo.container.impl.ClassPathItem;
import consulo.container.impl.PluginDescriptorImpl;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * @author VISTALL
 * @since 2019-11-23
 */
public class PluginClassLoaderFactory {
    @SuppressWarnings("unchecked")
    public static <C extends ClassLoader & PluginClassLoader> C create(Set<PluginId> enabledPluginIds, ClassLoader parent, PluginDescriptor pluginDescriptor) throws IOException {
        return create(enabledPluginIds, new ClassLoader[]{parent}, pluginDescriptor);
    }

    @SuppressWarnings("unchecked")
    public static <C extends ClassLoader & PluginClassLoader> C create(Set<PluginId> enabledPluginIds, ClassLoader[] parents, PluginDescriptor pluginDescriptor) throws IOException {
        PluginDescriptorImpl impl = (PluginDescriptorImpl) pluginDescriptor;

        List<ClassPathItem> classPathItems = impl.getClassPathItems(enabledPluginIds);
        List<URL> urls = new ArrayList<>(classPathItems.size());
        Map<URL, Set<String>> index = new HashMap<>();

        for (ClassPathItem item : classPathItems) {
            URL url = item.getPath().toURI().toURL();
            urls.add(url);

            if (impl.isEnabledIndex()) {
                index.put(url, item.getIndex());
            }
        }

        return (C) new PluginClassLoaderImpl(urls, impl.isEnabledIndex() ? index : null, parents, pluginDescriptor);
    }
}
