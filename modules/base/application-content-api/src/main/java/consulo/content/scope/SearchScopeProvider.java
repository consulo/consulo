// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.content.scope;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface SearchScopeProvider {
    @Nullable
    default String getDisplayName() {
        return null;
    }

    @Nonnull
    default List<SearchScope> getSearchScopes(@Nonnull Project project) {
        return Collections.emptyList();
    }

    /**
     * General project scopes are added after 'Project', 'Everything' but before 'Production', 'Tests', etc.
     *
     * @param project
     * @see PredefinedSearchScopeProvider
     */
    default List<SearchScope> getGeneralSearchScopes(@Nonnull Project project) {
        return Collections.emptyList();
    }
}
