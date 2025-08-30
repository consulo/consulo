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
package consulo.versionControlSystem.log.impl.internal.ui.action;

import consulo.annotation.component.ActionImpl;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogUiImpl;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "Vcs.Log.ExpandAll")
public class ExpandGraphAction extends CollapseOrExpandGraphAction {
    public ExpandGraphAction() {
        super(
            VersionControlSystemLogLocalize.actionCollapseLinearBranchesText(),
            VersionControlSystemLogLocalize.actionCollapseLinearBranchesDescription(),
            VersionControlSystemLogLocalize.actionCollapseMergesText(),
            VersionControlSystemLogLocalize.actionCollapseMergesDescription()
        );
    }

    @Override
    protected void executeAction(@Nonnull VcsLogUiImpl vcsLogUi) {
        vcsLogUi.expandAll();
    }
}
