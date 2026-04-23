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
package consulo.component.internal.inject;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-04-23
 */
public class NewBindingLoader {
    private final NewBindingCollector[] myCollectors;

    public NewBindingLoader(NewBindingCollector... collectors) {
        myCollectors = collectors;
    }

    public void init(List<Runnable> actions) {
        PluginManager.forEachEnabledPlugin(pluginDescriptor -> {
            ModuleLayer moduleLayer = pluginDescriptor.getModuleLayer();
            // this is optimization, since in legacy mode it's read META-INF and module-info services
            // and when load META-INF service loader will skip class, if it from named module (and we have all named)
            // that why load only from module-info
            if (moduleLayer != null) {
                loadInModuleMode(actions, moduleLayer, pluginDescriptor);
            }
            else {
                loadInLegacyMode(actions, pluginDescriptor);
            }
        });
    }

    private <T> void forEachCollector(List<Runnable> actions, Consumer<NewBindingCollector<T>> consumer) {
        for (NewBindingCollector bindingCollector : myCollectors) {
            actions.add(() -> consumer.accept(bindingCollector));
        }
    }

    private void loadInLegacyMode(List<Runnable> actions, PluginDescriptor pluginDescriptor) {
        forEachCollector(actions, collector -> {
            ServiceLoader loader = ServiceLoader.load(collector.getBindingClass(), pluginDescriptor.getPluginClassLoader());

            for (Object binding : loader) {
                ClassLoader classLoader = binding.getClass().getClassLoader();

                // if we loaded binding by another plugin - stop it, since we it will be called #getParent()
                if (classLoader != pluginDescriptor.getPluginClassLoader()) {
                    continue;
                }

                if (!collector.doRecordClass(binding.getClass())) {
                    throw new IllegalArgumentException("Duplicate registration of binding: " + binding.getClass());
                }

                try {
                    collector.process(binding);
                }
                catch (Throwable e) {
                    // TODO [VISTALL] log may not initialized here
                    e.printStackTrace();
                }
            }
        });
    }

    private void loadInModuleMode(List<Runnable> actions, ModuleLayer moduleLayer, PluginDescriptor pluginDescriptor) {
        forEachCollector(actions, collector -> {
            ServiceLoader loader = ServiceLoader.load(moduleLayer, collector.getBindingClass());

            Iterator<ServiceLoader.Provider<?>> iterator = loader.stream().iterator();
            while (iterator.hasNext()) {
                ServiceLoader.Provider<?> provider = iterator.next();

                ClassLoader classLoader = provider.type().getClassLoader();

                // if we loaded binding by another plugin - stop it, since we it will be called #getParent()
                if (classLoader != pluginDescriptor.getPluginClassLoader()) {
                    break;
                }

                if (!collector.doRecordClass(provider.type())) {
                    throw new IllegalArgumentException("Duplicate registration of binding: " + provider.type());
                }

                try {
                    Object binding = provider.get();
                    collector.process(binding);
                }
                catch (Throwable e) {
                    // TODO [VISTALL] log may not initialized here
                    e.printStackTrace();
                }
            }
        });
    }
}
