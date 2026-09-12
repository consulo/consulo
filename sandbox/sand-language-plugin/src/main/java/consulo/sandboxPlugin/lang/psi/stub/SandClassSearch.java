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
package consulo.sandboxPlugin.lang.psi.stub;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.StubIndex;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.psi.SandClass;

import java.util.Collection;

/**
 * Environment-aware class lookup over the condition-annotated stub index. The index holds
 * every declaration variant; {@link #active} is the default view (platform
 * {@code StubIndex.getActiveElements} consulting {@code SandStubVariantFilter});
 * {@link #matching} filters by an explicit requester environment, e.g. a resolve walking
 * in from an includer.
 */
public final class SandClassSearch {
    private SandClassSearch() {
    }

    @RequiredReadAction
    public static Collection<SandClass> active(Project project, String name) {
        return allVariants(project, name);
    }

    @RequiredReadAction
    public static Collection<SandClass> allVariants(Project project, String name) {
        return StubIndex.getElements(SandIndexKeys.SAND_CLASSES, name, project, GlobalSearchScope.projectScope(project), SandClass.class);
    }
}
