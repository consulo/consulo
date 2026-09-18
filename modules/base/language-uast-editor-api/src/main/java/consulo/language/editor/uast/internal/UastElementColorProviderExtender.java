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
package consulo.language.editor.uast.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.ComponentManager;
import consulo.component.extension.ExtensionExtender;
import consulo.language.editor.uast.UastElementColorProvider;
import consulo.language.psi.ElementColorProvider;
import consulo.language.uast.UastLanguagePlugin;

import java.util.function.Consumer;

/**
 * Fans every {@link UastElementColorProvider} out into one {@link UastElementColorProviderAdapter} per registered
 * {@link UastLanguagePlugin}, so that a single UAST color provider is registered as a regular
 * {@link ElementColorProvider} for every language that has a UAST binding.
 *
 * @author VISTALL
 * @since 2026-09-18
 */
@ExtensionImpl
public class UastElementColorProviderExtender implements ExtensionExtender<ElementColorProvider> {
    @Override
    public void extend(ComponentManager componentManager, Consumer<ElementColorProvider> consumer) {
        for (UastLanguagePlugin plugin : componentManager.getExtensionList(UastLanguagePlugin.class)) {
            for (UastElementColorProvider provider : componentManager.getExtensionList(UastElementColorProvider.class)) {
                consumer.accept(new UastElementColorProviderAdapter(provider, plugin));
            }
        }
    }

    @Override
    public Class<ElementColorProvider> getExtensionClass() {
        return ElementColorProvider.class;
    }

    @Override
    public boolean hasAnyExtensions(ComponentManager componentManager) {
        return componentManager.getExtensionPoint(UastElementColorProvider.class).hasAnyExtensions();
    }
}
