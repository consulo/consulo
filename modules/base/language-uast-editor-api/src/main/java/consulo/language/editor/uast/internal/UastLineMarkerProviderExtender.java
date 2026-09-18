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
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.uast.UastLineMarkerProvider;
import consulo.language.uast.UastLanguagePlugin;

import java.util.function.Consumer;

/**
 * Fans every {@link UastLineMarkerProvider} out into one {@link UastLineMarkerProviderAdapter} per registered
 * {@link UastLanguagePlugin}, so that a single UAST line marker provider is registered as a regular
 * {@link LineMarkerProvider} for every language that has a UAST binding.
 *
 * @author VISTALL
 * @since 2026-09-18
 */
@ExtensionImpl
public class UastLineMarkerProviderExtender implements ExtensionExtender<LineMarkerProvider> {
    @Override
    public void extend(ComponentManager componentManager, Consumer<LineMarkerProvider> consumer) {
        for (UastLanguagePlugin plugin : componentManager.getExtensionList(UastLanguagePlugin.class)) {
            for (UastLineMarkerProvider provider : componentManager.getExtensionList(UastLineMarkerProvider.class)) {
                consumer.accept(new UastLineMarkerProviderAdapter(provider, plugin));
            }
        }
    }

    @Override
    public Class<LineMarkerProvider> getExtensionClass() {
        return LineMarkerProvider.class;
    }

    @Override
    public boolean hasAnyExtensions(ComponentManager componentManager) {
        return componentManager.getExtensionPoint(UastLineMarkerProvider.class).hasAnyExtensions();
    }
}
