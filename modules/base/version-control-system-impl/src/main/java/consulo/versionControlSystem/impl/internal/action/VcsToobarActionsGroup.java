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
package consulo.versionControlSystem.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.application.dumb.DumbAware;
import consulo.versionControlSystem.impl.internal.action.CommonCheckinProjectAction;
import consulo.versionControlSystem.impl.internal.action.TabbedShowHistoryAction;
import consulo.versionControlSystem.impl.internal.change.commited.CommonUpdateProjectAction;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

/**
 * @author UNV
 * @since 2025-08-17
 */
@ActionImpl(
    id = "VcsToobarActions",
    children = {
        @ActionRef(type = VcsToolbarLabelAction.class),
        @ActionRef(type = CommonUpdateProjectAction.class),
        @ActionRef(type = CommonCheckinProjectAction.class),
        @ActionRef(type = CompareWithTheSameVersionAction.class),
        @ActionRef(type = TabbedShowHistoryAction.class),
        @ActionRef(id = IdeActions.CHANGES_VIEW_REVERT),
        @ActionRef(type = AnSeparator.class)
    },
    parents = @ActionParentRef(
        value = @ActionRef(id = "MainToolBarSettings"),
        anchor = ActionRefAnchor.BEFORE,
        relatedToAction = @ActionRef(id = "ShowSettings")
    )
)
public class VcsToobarActionsGroup extends DefaultActionGroup implements DumbAware {
    public VcsToobarActionsGroup() {
        super(ActionLocalize.groupVcstoobaractionsText(), false);
    }
}
