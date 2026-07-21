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
package consulo.it;

import consulo.application.Application;
import consulo.application.internal.StartupProgress;
import consulo.component.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingBindingLoader;
import consulo.component.internal.inject.NewBindingLoader;
import consulo.component.internal.inject.NewInjectingBindingCollector;
import consulo.component.internal.inject.NewTopicBindingCollector;
import consulo.component.internal.inject.TopicBindingLoader;
import consulo.container.internal.PathManagerHolder;
import consulo.container.internal.plugin.PluginHolderModificator;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginDescriptorStub;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.it.internal.HeadlessApplicationImpl;
import consulo.it.internal.HeadlessContainerPathManager;
import consulo.it.internal.HeadlessLoggerFactory;
import consulo.logging.internal.LoggerFactoryInitializer;
import consulo.localization.LocalizationManager;
import consulo.localization.internal.LocalizationManagerEx;
import consulo.localize.LocalizeManager;
import consulo.localize.internal.LocalizeManagerEx;
import consulo.util.lang.ref.SimpleReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Boots a real headless {@link Application} for integration tests: every {@code @ServiceImpl}
 * binding is discovered straight from the Maven dependency classpath, then {@link HeadlessApplicationImpl}
 * is constructed. No disk plugins layout and no {@code ApplicationStarter}/{@code StartupUtil} ceremony.
 * <p>
 * Instead of loading plugin descriptors from disk, a single synthetic descriptor whose classloader is
 * the whole application classloader is fed to {@link PluginHolderModificator}, so the real
 * {@code PluginManagerInternalImpl} serves it and {@link NewBindingLoader} sees all bindings on the classpath.
 *
 * @author VISTALL
 */
public final class HeadlessApplicationBuilder {
    private HeadlessApplicationBuilder() {
    }

    public static Application build() {
        // no native fsnotifier in a headless classpath run: disable the watcher and short-circuit
        // the executable lookup (it would otherwise consult the synthetic plugin's null nioPath)
        System.setProperty("consulo.filewatcher.disabled", "true");
        System.setProperty("consulo.filewatcher.executable.path", "fsnotifier-headless-disabled");

        if (!LoggerFactoryInitializer.isInitialized()) {
            LoggerFactoryInitializer.setFactory(new HeadlessLoggerFactory());
        }

        if (PathManagerHolder.getInstance() == null) {
            PathManagerHolder.setInstance(new HeadlessContainerPathManager());
        }

        initSyntheticPlugin();

        LocalizationManagerEx localizationManager = (LocalizationManagerEx) LocalizationManager.get();
        LocalizeManagerEx localizeManager = (LocalizeManagerEx) LocalizeManager.get();

        NewInjectingBindingCollector injectingBindingCollector = new NewInjectingBindingCollector();
        NewTopicBindingCollector topicBindingCollector = new NewTopicBindingCollector();
        NewBindingLoader loader = new NewBindingLoader(injectingBindingCollector, topicBindingCollector);

        List<Runnable> actions = new ArrayList<>();
        loader.init(actions);

        localizationManager.initialize();

        actions.parallelStream().forEach(Runnable::run);

        localizeManager.afterInit();

        InjectingBindingLoader injectingBindingLoader = new InjectingBindingLoader(
            injectingBindingCollector.getServices(),
            injectingBindingCollector.getExtensions(),
            injectingBindingCollector.getTopics(),
            injectingBindingCollector.getActions()
        );

        TopicBindingLoader topicBindingLoader = new TopicBindingLoader(topicBindingCollector.getBindings());

        ComponentBinding componentBinding = new ComponentBinding(injectingBindingLoader, topicBindingLoader);

        return new HeadlessApplicationImpl(componentBinding, SimpleReference.<StartupProgress>create());
    }

    private static void initSyntheticPlugin() {
        if (PluginHolderModificator.isInitialized()) {
            return;
        }

        PluginDescriptor plugin = new PluginDescriptorStub() {
            @Override
            public PluginId getPluginId() {
                return PluginIds.CONSULO_BASE;
            }

            @Override
            public ClassLoader getPluginClassLoader() {
                return HeadlessApplicationBuilder.class.getClassLoader();
            }
        };

        PluginHolderModificator.initialize(List.of(plugin));
        PluginHolderModificator.setPluginLoadOrder(Map.of(PluginIds.CONSULO_BASE, 0));
    }
}
