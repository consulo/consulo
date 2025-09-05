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
package consulo.versionControlSystem.impl.internal.change.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.versionControlSystem.impl.internal.change.commited.ChangeListDetailsAction;
import consulo.versionControlSystem.impl.internal.change.commited.FilterCommittedAction;

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
