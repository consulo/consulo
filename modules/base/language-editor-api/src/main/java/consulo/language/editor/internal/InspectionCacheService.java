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
package consulo.language.editor.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.editor.inspection.InspectionTool;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2025-10-13
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class InspectionCacheService {
    private static final ExtensionPointCacheKey<InspectionTool, InspectionCache> CACHE_KEY = ExtensionPointCacheKey.create("InspectionCache", inspectionToolExtensionWalker -> {
        InspectionCache cache = new InspectionCache();
        inspectionToolExtensionWalker.walk(cache::eat);
        cache = cache.lock();
        return cache;
    });

    public static InspectionCacheService getInstance() {
        return Application.get().getInstance(InspectionCacheService.class);
    }

    private final Application myApplication;

    @Inject
    public InspectionCacheService(Application application) {
        myApplication = application;
    }

    @Nonnull
    public InspectionCache get() {
        return myApplication.getExtensionPoint(InspectionTool.class).getOrBuildCache(CACHE_KEY);
    }
}
