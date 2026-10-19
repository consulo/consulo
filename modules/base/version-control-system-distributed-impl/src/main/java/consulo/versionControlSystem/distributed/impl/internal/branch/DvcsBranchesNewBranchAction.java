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
package consulo.versionControlSystem.distributed.impl.internal.branch;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.AnAction;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopupModel;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsRef;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
@ActionImpl(id = "Dvcs.Log.Branches.NewBranch")
public class DvcsBranchesNewBranchAction extends DvcsBranchesDelegatingAction {
    public DvcsBranchesNewBranchAction() {
        super(DistributedVcsLocalize.actionNewBranchText(), PlatformIconGroup.generalAdd());
    }

    @Override
    protected @Nullable AnAction createDelegate(DvcsBranchesTreePopupModel<?> model, @Nullable DvcsRef selectedRef) {
        return model.createNewBranchAction();
    }
}
