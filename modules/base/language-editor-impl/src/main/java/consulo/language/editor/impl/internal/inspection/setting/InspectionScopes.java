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
package consulo.language.editor.impl.internal.inspection.setting;

import consulo.content.internal.scope.AllScopeHolder;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.scope.NonProjectFilesScope;
import consulo.project.Project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public final class InspectionScopes {
    private InspectionScopes() {
    }

    public static List<NamedScope> getPredefinedScopes(Project project) {
        String allScopeId = AllScopeHolder.getInstance().getAllScope().getScopeId();

        List<NamedScope> result = new ArrayList<>();
        for (NamedScopesHolder holder : NamedScopesHolder.getAllNamedScopeHolders(project)) {
            for (NamedScope scope : holder.getPredefinedScopes()) {
                if (scope instanceof NonProjectFilesScope || scope.getScopeId().equals(allScopeId)) {
                    continue;
                }
                result.add(scope);
            }
        }
        return result;
    }

    public static List<NamedScope> getCustomScopes(Project project) {
        List<NamedScope> result = new ArrayList<>();
        for (NamedScopesHolder holder : NamedScopesHolder.getAllNamedScopeHolders(project)) {
            Collections.addAll(result, holder.getEditableScopes());
        }
        return result;
    }

    public static List<String> getOrderedScopeIds(InspectionProfileImpl profile, Project project) {
        String allScopeId = AllScopeHolder.getInstance().getAllScope().getScopeId();

        List<String> scopes = new ArrayList<>();
        for (NamedScopesHolder holder : NamedScopesHolder.getAllNamedScopeHolders(project)) {
            for (NamedScope scope : holder.getScopes()) {
                if (!(scope instanceof NonProjectFilesScope) && !scope.getScopeId().equals(allScopeId)) {
                    scopes.add(scope.getScopeId());
                }
            }
        }
        scopes.sort(new ScopeOrderComparator(profile));
        return scopes;
    }
}
