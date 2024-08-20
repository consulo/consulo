/*
 * Copyright 2013-2023 consulo.io
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
package consulo.component.extension.preview;

import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22/01/2023
 */
public record ExtensionPreview(@Nonnull PluginId apiPluginId,
                               @Nonnull String apiClassName,
                               @Nonnull PluginId implPluginId,
                               @Nonnull String implId) {

    public static <T> ExtensionPreview of(@Nonnull Class<T> apiClass, @Nonnull String implId) {
        return new ExtensionPreview(getPluginStrict(apiClass), apiClass.getName(), getPluginStrict(apiClass), implId);
    }

    public static <T> ExtensionPreview of(@Nonnull Class<T> apiClass, @Nonnull String implId, @Nonnull T implClass) {
        return new ExtensionPreview(getPluginStrict(apiClass), apiClass.getName(), getPluginStrict(implClass.getClass()), implId);
    }

    @Nonnull
    private static PluginId getPluginStrict(Class<?> clazz) {
        PluginId pluginId = PluginManager.getPluginId(clazz);
        if (pluginId == null) {
            throw new IllegalArgumentException("No plugin for class " + clazz.getName());
        }
        return pluginId;
    }
}
