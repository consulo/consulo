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
package consulo.ui.ex.impl.internal.font;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.extension.ExtensionWalker;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.ui.ex.font.BundledFont;
import consulo.ui.ex.font.BundledFontProvider;
import consulo.ui.ex.internal.BundledFontRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
@Singleton
@ServiceImpl
public class BundledFontRegistryImpl implements BundledFontRegistry {
    private static final ExtensionPointCacheKey<BundledFontProvider, Map<PluginId, List<BundledFont>>> FONTS =
        ExtensionPointCacheKey.create("BundledFontRegistry", BundledFontRegistryImpl::collectFonts);

    private final Application myApplication;

    @Inject
    public BundledFontRegistryImpl(Application application) {
        myApplication = application;
    }

    @Override
    public Map<PluginId, List<BundledFont>> getFonts() {
        return myApplication.getExtensionPoint(BundledFontProvider.class).getOrBuildCache(FONTS);
    }

    private static Map<PluginId, List<BundledFont>> collectFonts(ExtensionWalker<BundledFontProvider> walker) {
        Map<PluginId, List<BundledFont>> fonts = new LinkedHashMap<>();
        walker.walk(provider -> {
            PluginId pluginId = PluginManager.getPluginId(provider.getClass());

            provider.registerFonts(fonts.computeIfAbsent(pluginId == null ? PluginIds.CONSULO_BASE : pluginId, id -> new ArrayList<>())::add);
        });
        return Collections.unmodifiableMap(fonts);
    }
}
