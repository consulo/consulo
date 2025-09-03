/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.diagram.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.ComponentManager;
import consulo.component.extension.ExtensionExtender;
import consulo.component.extension.ExtensionPoint;
import consulo.diagram.GraphProvider;
import consulo.language.editor.diagram.LanguageGraphProvider;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2025-09-03
 */
@ExtensionImpl
public class LanguageGraphProviderExtender implements ExtensionExtender<GraphProvider> {
    @Override
    public void extend(@Nonnull ComponentManager componentManager, @Nonnull Consumer<GraphProvider> consumer) {
        //noinspection GetExtensionPoint
        ExtensionPoint<LanguageGraphProvider> point = componentManager.getExtensionPoint(LanguageGraphProvider.class);

        point.forEach(it -> consumer.accept(new LanguageGraphProviderImpl(it)));
    }

    @Nonnull
    @Override
    public Class<GraphProvider> getExtensionClass() {
        return GraphProvider.class;
    }

    @Override
    public boolean hasAnyExtensions(ComponentManager componentManager) {
        return true;
    }
}
