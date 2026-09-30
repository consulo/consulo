/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package consulo.diagram;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public final class DiagramUtil {
    private DiagramUtil() {
    }

    public static boolean hasNotNull(@Nullable Object... objects) {
        for (Object object : objects) {
            if (object != null) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static Object[] getNodeItemsForCategory(DiagramProvider<?> diagramProvider, @Nullable Object parent, @Nullable DiagramCategory category) {
        List<Object> result = new ArrayList<>();
        if (parent != null && category != null) {
            DiagramNodeContentManager contentManager = diagramProvider.getNodeContentManager();
            DiagramElementManager<Object> elementManager = (DiagramElementManager<Object>) diagramProvider.getElementManager();
            for (Object element : elementManager.getNodeItems(parent)) {
                if (contentManager.isInCategory(element, category)) {
                    result.add(element);
                }
            }
        }
        return result.toArray();
    }

    @SuppressWarnings("rawtypes")
    public static DiagramElementsProvider[] getElementProviders(@Nullable DiagramProvider<?> provider) {
        DiagramExtras<?> extras = provider == null ? null : provider.getExtras();
        if (extras == null) {
            return DiagramElementsProvider.EMPTY_ARRAY;
        }
        DiagramElementsProvider[] providers = extras.getElementsProviders();
        return providers.length == 0 ? DiagramElementsProvider.EMPTY_ARRAY : providers;
    }
}
