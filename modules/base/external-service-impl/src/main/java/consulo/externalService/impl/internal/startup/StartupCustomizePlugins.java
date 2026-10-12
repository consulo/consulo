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
package consulo.externalService.impl.internal.startup;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The plugins of the repository as the startup wizard needs them: by id, and with what they need and what needs them.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
final class StartupCustomizePlugins {
    private final Map<PluginId, PluginDescriptor> myById = new LinkedHashMap<>();

    StartupCustomizePlugins(List<PluginDescriptor> repositoryPlugins) {
        for (PluginDescriptor descriptor : repositoryPlugins) {
            myById.put(descriptor.getPluginId(), descriptor);
        }
    }

    @Nullable PluginDescriptor find(PluginId id) {
        return myById.get(id);
    }

    static boolean isInstalled(PluginId id) {
        return PluginManager.findPlugin(id) != null || PluginIds.isPlatformPlugin(id);
    }

    /**
     * The plugin and every plugin of the repository it needs, directly or not, leaving out what is installed already.
     */
    Set<PluginId> withDependencies(PluginId id) {
        Set<PluginId> result = new LinkedHashSet<>();
        Deque<PluginId> queue = new ArrayDeque<>();
        queue.add(id);

        while (!queue.isEmpty()) {
            PluginId next = queue.poll();
            if (isInstalled(next) || !result.add(next)) {
                continue;
            }

            PluginDescriptor descriptor = myById.get(next);
            if (descriptor != null) {
                queue.addAll(List.of(descriptor.getDependentPluginIds()));
            }
        }
        return result;
    }

    /**
     * Every plugin of the repository which needs the plugin, directly or not.
     */
    Set<PluginId> dependents(PluginId id) {
        Set<PluginId> result = new LinkedHashSet<>();
        Deque<PluginId> queue = new ArrayDeque<>();
        queue.add(id);

        while (!queue.isEmpty()) {
            PluginId next = queue.poll();
            for (PluginDescriptor descriptor : myById.values()) {
                if (List.of(descriptor.getDependentPluginIds()).contains(next) && result.add(descriptor.getPluginId())) {
                    queue.add(descriptor.getPluginId());
                }
            }
        }
        return result;
    }
}
