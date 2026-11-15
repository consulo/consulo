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
package consulo.language.index.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.Application;
import consulo.application.plugin.DynamicPluginChange;
import consulo.application.plugin.DynamicPluginListener;
import consulo.container.plugin.PluginDescriptor;
import consulo.index.io.ID;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.StubElementTypeHolder;
import consulo.language.psi.stub.StubIndexExtension;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reloads the file based and the stub indexes when a plugin session changes their extensions: the indexes of an
 * unloaded plugin are shut down while its classes are still available, the ones of a loaded plugin are registered
 * from the extension points after the session.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
@TopicImpl(ComponentScope.APPLICATION)
public class IndexDynamicPluginListener implements DynamicPluginListener {
    private static final List<Class<?>> INDEX_EXTENSION_CLASSES =
        List.of(FileBasedIndexExtension.class, StubIndexExtension.class, StubElementTypeHolder.class);

    private final Application myApplication;

    private @Nullable FileBasedIndexTumbler myTumbler;
    /**
     * True when {@link #beforePluginsChange} turned the indexes off, the {@link #pluginsChanged} of the session turns them on.
     */
    private boolean myTurnedOff;
    private long @Nullable [] myModificationCounts;

    @Inject
    public IndexDynamicPluginListener(Application application) {
        myApplication = application;
    }

    @Override
    public synchronized void beforePluginsChange(DynamicPluginChange change) {
        Set<ClassLoader> extensionClassLoaders = collectExtensionClassLoaders();
        myModificationCounts = getModificationCounts();

        boolean unloadsIndexes = false;
        for (PluginDescriptor plugin : change.getUnloading()) {
            if (extensionClassLoaders.contains(plugin.getPluginClassLoader())) {
                unloadsIndexes = true;
                break;
            }
        }

        if (!unloadsIndexes) {
            return;
        }

        try {
            getTumbler().turnOff();
        }
        finally {
            // the tumbler counts a failed turn off too, so the turn on must follow
            myTurnedOff = true;
        }
    }

    @Override
    public void beforePluginUnload(PluginDescriptor pluginDescriptor) {
        // the plugin registers its index ids again when it is loaded
        ClassLoader classLoader = pluginDescriptor.getPluginClassLoader();
        myApplication.getExtensionPoint(FileBasedIndexExtension.class).forEachExtensionSafe(extension -> {
            if (extension.getClass().getClassLoader() == classLoader) {
                ID.unloadId(extension.getName());
            }
        });
        myApplication.getExtensionPoint(StubIndexExtension.class).forEachExtensionSafe(extension -> {
            if (extension.getClass().getClassLoader() == classLoader) {
                ID.unloadId(extension.getKey());
            }
        });
        ID.unloadIdsOf(classLoader);
    }

    @Override
    public synchronized void pluginsChanged(DynamicPluginChange change) {
        long[] modificationCounts = myModificationCounts;
        myModificationCounts = null;

        if (myTurnedOff) {
            myTurnedOff = false;
            getTumbler().turnOn();
            return;
        }

        if (modificationCounts == null || Arrays.equals(modificationCounts, getModificationCounts())) {
            return;
        }

        FileBasedIndexTumbler tumbler = getTumbler();
        try {
            tumbler.turnOff();
        }
        finally {
            tumbler.turnOn();
        }
    }

    private FileBasedIndexTumbler getTumbler() {
        FileBasedIndexTumbler tumbler = myTumbler;
        if (tumbler == null) {
            myTumbler = tumbler = new FileBasedIndexTumbler("Plugin loaded/unloaded");
        }
        return tumbler;
    }

    private Set<ClassLoader> collectExtensionClassLoaders() {
        Set<ClassLoader> classLoaders = new HashSet<>();
        for (Class<?> extensionClass : INDEX_EXTENSION_CLASSES) {
            myApplication.getExtensionPoint(extensionClass)
                .forEachExtensionSafe(extension -> {
                    ClassLoader classLoader = extension.getClass().getClassLoader();
                    if (classLoader != null) {
                        classLoaders.add(classLoader);
                    }
                });
        }
        return classLoaders;
    }

    private long[] getModificationCounts() {
        long[] counts = new long[INDEX_EXTENSION_CLASSES.size()];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = myApplication.getExtensionPoint(INDEX_EXTENSION_CLASSES.get(i)).getModificationCount();
        }
        return counts;
    }
}
