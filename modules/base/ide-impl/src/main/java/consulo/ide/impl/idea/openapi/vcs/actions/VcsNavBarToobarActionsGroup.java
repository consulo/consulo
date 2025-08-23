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
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.dvcs.push.VcsPushAction;
import consulo.ide.impl.idea.openapi.vcs.update.CommonUpdateProjectAction;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-08-17
 */
@ActionImpl(
    id = "VcsNavBarToobarActions",
    children = {
        @ActionRef(type = VcsToolbarLabelAction.class),
        @ActionRef(type = CommonUpdateProjectAction.class),
        @ActionRef(type = VcsPushAction.class),
        @ActionRef(type = CommonCheckinProjectAction.class),
        @ActionRef(type = TabbedShowHistoryAction.class),
        @ActionRef(type = AnSeparator.class)
    },
    parents = @ActionParentRef(value = @ActionRef(id = "NavBarVcsGroup"), anchor = ActionRefAnchor.FIRST)
)
public class VcsNavBarToobarActionsGroup extends DefaultActionGroup implements DumbAware {
    public VcsNavBarToobarActionsGroup() {
        super(ActionLocalize.groupVcsnavbartoobaractionsText(), false);
    }
}
