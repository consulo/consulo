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
package consulo.ide.impl.idea.openapi.vcs.changes.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.ide.actions.EditSourceAction;
import consulo.ide.impl.idea.openapi.vcs.actions.VersionControlsGroup;
import consulo.versionControlSystem.impl.internal.action.ShowDiffAction;
import consulo.ide.impl.idea.openapi.vcs.changes.shelf.ShelveChangesAction;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.versionControlSystem.impl.internal.change.action.AddUnversionedAction;
import consulo.versionControlSystem.impl.internal.change.action.DeleteUnversionedFilesAction;
import consulo.versionControlSystem.impl.internal.change.action.MoveChangesToAnotherListAction;
import consulo.versionControlSystem.impl.internal.change.action.RemoveChangeListAction;

/**
 * @author UNV
 * @since 2025-08-18
 */
@ActionImpl(
    id = "ChangesViewPopupMenu",
    children = {
        @ActionRef(type = CommitAction.class),
        @ActionRef(type = RollbackAction.class),
        @ActionRef(type = MoveChangesToAnotherListAction.class),
        @ActionRef(type = ShowDiffAction.class),
        @ActionRef(type = EditSourceAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = DeleteUnversionedFilesAction.class),
        @ActionRef(type = AddUnversionedAction.class),
        @ActionRef(type = IgnoreUnversionedAction.class),
        @ActionRef(type = ScheduleForRemovalAction.class),
        @ActionRef(type = EditAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = AddChangeListAction.class),
        @ActionRef(type = RemoveChangeListAction.class),
        @ActionRef(type = SetDefaultChangeListAction.class),
        @ActionRef(type = RenameChangeListAction.class),
        @ActionRef(type = CreatePatchAction.class),
        @ActionRef(type = ShelveChangesAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = RefreshAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = VersionControlsGroup.class)
    }
)
public class ChangesViewPopupMenuGroup extends DefaultActionGroup implements DumbAware {
    public ChangesViewPopupMenuGroup() {
        super(ActionLocalize.groupChangesviewpopupmenuText(), false);
    }
}
