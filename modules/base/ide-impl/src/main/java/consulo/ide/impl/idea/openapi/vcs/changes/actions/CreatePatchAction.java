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

import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.change.CommitExecutor;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.CreatePatchCommitExecutor;
import consulo.project.Project;

import jakarta.annotation.Nullable;

/**
 * @author yole
 */
public class CreatePatchAction extends AbstractCommitChangesAction {
  @Override
  protected String getActionName(VcsContext dataContext) {
    return VcsBundle.message("create.patch.commit.action.title");
  }

  @Override
  protected String getMnemonicsFreeActionName(VcsContext context) {
    return getActionName(context);
  }

  @Override
  @Nullable
  protected CommitExecutor getExecutor(Project project) {
    return new CreatePatchCommitExecutor(project);
  }
}