/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.CreatePatchFromChangesAction;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.RevertChangeListAction;
import consulo.versionControlSystem.impl.internal.change.commited.ChangeListDetailsAction;
import consulo.ide.impl.idea.openapi.vcs.changes.committed.ClearCommittedAction;
import consulo.ide.impl.idea.openapi.vcs.changes.committed.FilterCommittedAction;
import consulo.ide.impl.idea.openapi.vcs.changes.committed.RefreshCommittedAction;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-08-17
 */
@ActionImpl(
    id = "CommittedChangesToolbar",
    children = {
        @ActionRef(type = RefreshCommittedAction.class),
        @ActionRef(type = FilterCommittedAction.class),
        @ActionRef(type = ChangeListDetailsAction.class),
        @ActionRef(type = CreatePatchFromChangesAction.class),
        @ActionRef(type = RevertChangeListAction.class),
        @ActionRef(type = ClearCommittedAction.class)
    }
)
public class CommittedChangesToolbarGroup extends DefaultActionGroup implements DumbAware {
    public CommittedChangesToolbarGroup() {
        super(ActionLocalize.groupCommittedchangestoolbarText(), false);
    }
}
