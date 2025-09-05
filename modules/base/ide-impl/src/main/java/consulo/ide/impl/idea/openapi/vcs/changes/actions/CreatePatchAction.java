/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
import consulo.versionControlSystem.impl.internal.change.patch.CreatePatchCommitExecutor;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.change.CommitExecutor;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
@ActionImpl(id = "ChangesView.CreatePatch")
public class CreatePatchAction extends AbstractCommitChangesAction {
    public CreatePatchAction() {
        super(
            ActionLocalize.actionChangesviewCreatepatchText(),
            ActionLocalize.actionChangesviewCreatepatchDescription(),
            PlatformIconGroup.filetypesPatch()
        );
    }

    @Override
    protected String getActionName(@Nonnull VcsContext dataContext) {
        return VcsLocalize.createPatchCommitActionTitle().get();
    }

    @Override
    protected String getMnemonicsFreeActionName(@Nonnull VcsContext context) {
        return getActionName(context);
    }

    @Nullable
    @Override
    protected CommitExecutor getExecutor(@Nonnull Project project) {
        return new CreatePatchCommitExecutor(project);
    }
}