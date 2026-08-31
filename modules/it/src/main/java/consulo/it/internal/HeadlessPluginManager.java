/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.container.internal.PluginManagerInternal;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginDescriptorStub;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * Synthetic single-plugin manager for headless integration tests. One descriptor whose classloader is
 * the whole application classloader, so {@code NewBindingLoader} discovers every {@code @ServiceImpl}
 * binding from the Maven dependencies. Unlike the real manager, {@link #getPlugin(Class)} always maps
 * any class to this synthetic plugin (there are no per-plugin {@code PluginClassLoader}s on a flat test
 * classpath), which the extension-point machinery requires.
 *
 * @author VISTALL
 */
public class HeadlessPluginManager implements PluginManagerInternal {
    private final PluginDescriptor myTestPlugin = new PluginDescriptorStub() {
        @Override
        public PluginId getPluginId() {
            return PluginIds.CONSULO_BASE;
        }

        @Override
        public ClassLoader getPluginClassLoader() {
            return HeadlessPluginManager.class.getClassLoader();
        }
    };

    private final List<PluginDescriptor> myPlugins = List.of(myTestPlugin);

    @Override
    public List<PluginDescriptor> getPlugins() {
        return myPlugins;
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public File getPluginPath(Class<?> pluginClass) {
        return null;
    }

    @Override
    public @Nullable PluginDescriptor getPlugin(Class<?> pluginClass) {
        return myTestPlugin;
    }
}
