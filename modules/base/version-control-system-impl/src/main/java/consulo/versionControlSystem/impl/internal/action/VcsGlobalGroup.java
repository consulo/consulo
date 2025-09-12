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
import consulo.annotation.component.ActionRef;
import consulo.ui.ex.action.AnSeparator;
import consulo.versionControlSystem.impl.internal.change.action.CreatePatchAction;
import consulo.versionControlSystem.impl.internal.change.action.RefreshAction;
import consulo.versionControlSystem.impl.internal.change.action.ShelveChangesAction;
import consulo.versionControlSystem.impl.internal.change.commited.CommonUpdateProjectAction;
import consulo.versionControlSystem.localize.VcsLocalize;

/**
 * @author UNV
 * @since 2025-09-11
 */
@ActionImpl(
    id = "VcsGlobalGroup",
    children = {
        @ActionRef(type = VcsQuickListPopupAction.class),
        @ActionRef(type = CommonCheckinProjectAction.class),
        @ActionRef(type = CommonUpdateProjectAction.class),
        @ActionRef(type = CommonIntegrateProjectAction.class),
        @ActionRef(type = RefreshAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = VcsSpecificGroup.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = CreatePatchAction.class),
        @ActionRef(id = "ChangesView.ApplyPatch"),
        @ActionRef(id = "ChangesView.ApplyPatchFromClipboard"),
        @ActionRef(type = ShelveChangesAction.class)
    }
)
public class VcsGlobalGroup extends VcsActionGroup {
    public VcsGlobalGroup() {
        super(VcsLocalize.groupGlobalGroupText(), false);
    }
}
