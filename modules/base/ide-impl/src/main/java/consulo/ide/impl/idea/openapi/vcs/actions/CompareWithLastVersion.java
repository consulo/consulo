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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.versionControlSystem.diff.DiffProvider;
import consulo.ide.impl.idea.openapi.vcs.impl.VcsBackgroundableActions;
import consulo.virtualFileSystem.VirtualFile;

@ActionImpl(id = "Compare.LastVersion")
public class CompareWithLastVersion extends AbstractShowDiffAction {
    public CompareWithLastVersion() {
        super(ActionLocalize.actionCompareLastversionText());
    }

    @Override
    protected VcsBackgroundableActions getKey() {
        return VcsBackgroundableActions.COMPARE_WITH;
    }

    @Override
    protected DiffActionExecutor getExecutor(DiffProvider diffProvider, VirtualFile selectedFile, Project project) {
        return new DiffActionExecutor.DeletionAwareExecutor(diffProvider, selectedFile, project, getKey());
    }
}
