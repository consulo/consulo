/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package consulo.language.editor.impl.internal.inspection.scheme;

import consulo.application.Application;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.internal.InspectionCache;
import consulo.language.editor.internal.InspectionCacheService;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * @author max
 */
public interface InspectionToolRegistrar {
    static Supplier<Collection<InspectionToolWrapper<?>>> fromApplication(Application application) {
        return () -> {
            InspectionCache cache = application.getInstance(InspectionCacheService.class).get();
            return cache.getToolWrappers();
        };
    }

    static Supplier<Collection<InspectionToolWrapper<?>>> fromService(InspectionCacheService cacheService) {
        return () -> cacheService.get().getToolWrappers();
    }
}
