/*
 * Copyright 2013-2024 consulo.io
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
package consulo.project.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.project.PluginAdvertiserExtension;
import consulo.project.internal.UnknownFeaturesCollector;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.Set;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2024-08-17
 */
@ExtensionImpl
public class DefaultPluginAdvertiserExtension implements PluginAdvertiserExtension {
    private final UnknownFeaturesCollector myUnknownFeaturesCollector;

    @Inject
    public DefaultPluginAdvertiserExtension(UnknownFeaturesCollector unknownFeaturesCollector) {
        myUnknownFeaturesCollector = unknownFeaturesCollector;
    }

    @Override
    public void extend(@Nonnull Consumer<ExtensionPreview> consumer) {
        myUnknownFeaturesCollector.getUnknownExtensions().forEach(consumer);
    }

    @Nonnull
    @Override
    public Set<Class<?>> acceptExtensionAPIs() {
        return Set.of();
    }
}
