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
package consulo.language.editor.impl.internal.hierarchy.scope;

import consulo.language.editor.hierarchy.HierarchyScope;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * @author UNV
 * @since 2025-04-24
 */
public final class HierarchyScopes {
    private static final Map<String, HierarchyScope> BASIC_SCOPES = Map.of(
        AllHierarchyScope.INSTANCE.getId(), AllHierarchyScope.INSTANCE,
        ProductionHierarchyScope.INSTANCE.getId(), ProductionHierarchyScope.INSTANCE,
        TestHierarchyScope.INSTANCE.getId(), TestHierarchyScope.INSTANCE,
        ThisClassHierarchyScope.INSTANCE.getId(), ThisClassHierarchyScope.INSTANCE
    );

    public static HierarchyScope find(Project project, @Nullable String id) {
        if (id == null) {
            return AllHierarchyScope.INSTANCE;
        }

        HierarchyScope basicScope = BASIC_SCOPES.get(id);
        if (basicScope != null) {
            return basicScope;
        }

        HierarchyScope namedScope = NamedHierarchyScope.find(project, id);
        return namedScope != null ? namedScope : AllHierarchyScope.INSTANCE;
    }

    public static Map<String, HierarchyScope> getBasicScopes() {
        return BASIC_SCOPES;
    }

    private HierarchyScopes() {
    }
}
