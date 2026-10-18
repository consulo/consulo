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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.versionControlSystem.ui.VcsChangesBrowser;
import consulo.versionControlSystem.ui.VcsChangesBrowserBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedVcsChangesBrowserBuilder implements VcsChangesBrowserBuilder {
    private final Project myProject;
    private final List<AnAction> myToolbarActions = new ArrayList<>();
    private LocalizeValue myEmptyText = LocalizeValue.empty();
    private boolean myWithRollback;

    public UnifiedVcsChangesBrowserBuilder(Project project) {
        myProject = project;
    }

    @Override
    public VcsChangesBrowserBuilder emptyText(LocalizeValue text) {
        myEmptyText = text;
        return this;
    }

    @Override
    public VcsChangesBrowserBuilder toolbarAction(AnAction action) {
        myToolbarActions.add(action);
        return this;
    }

    @Override
    public VcsChangesBrowserBuilder withRollback() {
        myWithRollback = true;
        return this;
    }

    @RequiredUIAccess
    @Override
    public VcsChangesBrowser build(Disposable parentDisposable) {
        return new UnifiedVcsChangesBrowser(myProject, myEmptyText, List.copyOf(myToolbarActions), myWithRollback, parentDisposable);
    }
}
