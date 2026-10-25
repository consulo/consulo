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
package consulo.versionControlSystem.workspace;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.versionControlSystem.change.ChangesViewContentFactory;
import consulo.versionControlSystem.change.ChangesViewContentProvider;

/**
 * Registers a VCS's workspace view as a tab of the version control tool window: extend it with an {@code @ExtensionImpl}.
 */
public abstract class VcsWorkspaceContentFactory implements ChangesViewContentFactory {
    protected final Project myProject;

    protected VcsWorkspaceContentFactory(Project project) {
        myProject = project;
    }

    protected abstract VcsWorkspaceProvider getProvider();

    @Override
    public String getId() {
        return getProvider().getId();
    }

    @Override
    public LocalizeValue getTabName() {
        return getProvider().getFeatureName();
    }

    @Override
    public boolean isAvailable() {
        return getProvider().isTabAvailable();
    }

    @Override
    public int getOrderWeight() {
        return getProvider().getOrderWeight();
    }

    @Override
    public boolean isCloseable() {
        return true;
    }

    @Override
    public void contentClosedByUser() {
        getProvider().tabClosedByUser();
    }

    @Override
    public ChangesViewContentProvider create() {
        return myProject.getInstance(VcsWorkspaceViewFactory.class).create(getProvider());
    }
}
