/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.content.scope;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class PredefinedSearchScopeProvider {
    public static PredefinedSearchScopeProvider getInstance() {
        return Application.get().getInstance(PredefinedSearchScopeProvider.class);
    }

    /**
     * @param suggestSearchInLibs add <i>Project and Libraries</i> scope
     * @param prevSearchFiles     add <i>Files in Previous Search Result</i> instead of <i>Previous Search Result</i>
     *                            (only if {@code usageView == true})
     * @param currentSelection    add <i>Selection</i> scope if text is selected in the editor
     * @param usageView           add <i>Previous Search Result</i> and <i>Hierarchy 'X' (visible nodes only)</i> scopes
     *                            if there are search results or hierarchies open
     * @param showEmptyScopes     add <i>Current File</i> and <i>Open Files</i> scopes even if there are no files open
     */
    public abstract List<SearchScope> getPredefinedScopes(
        @Nonnull Project project,
        @Nullable DataContext dataContext,
        boolean suggestSearchInLibs,
        boolean prevSearchFiles,
        boolean currentSelection,
        boolean usageView,
        boolean showEmptyScopes
    );

    public final List<SearchScope> getPredefinedScopes(
        @Nonnull Project project,
        @Nullable DataContext dataContext,
        boolean suggestSearchInLibs,
        boolean prevSearchFiles,
        boolean currentSelection,
        boolean usageView
    ) {
        return getPredefinedScopes(
            project,
            dataContext,
            suggestSearchInLibs,
            prevSearchFiles,
            currentSelection,
            usageView,
            false
        );
    }
}
