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
package consulo.versionControlSystem.impl.internal.update;

import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.base.FilePathImpl;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.update.ActionInfo;
import consulo.virtualFileSystem.VirtualFile;

import java.util.ArrayList;
import java.util.List;

public interface ScopeInfo {
    FilePath[] getRoots(VcsContext context, ActionInfo actionInfo);

    String getScopeName(VcsContext dataContext, ActionInfo actionInfo);

    boolean filterExistsInVcs();

    ScopeInfo PROJECT = new ScopeInfo() {
        @Override
        public String getScopeName(VcsContext dataContext, ActionInfo actionInfo) {
            return VcsLocalize.updateProjectScopeName().get();
        }

        @Override
        public boolean filterExistsInVcs() {
            return true;
        }

        @Override
        public FilePath[] getRoots(VcsContext context, ActionInfo actionInfo) {
            List<FilePath> result = new ArrayList<>();
            Project project = context.getProject();
            ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
            AbstractVcs[] vcses = vcsManager.getAllActiveVcss();
            for (AbstractVcs vcs : vcses) {
                if (actionInfo.getEnvironment(vcs) != null) {
                    VirtualFile[] files = vcsManager.getRootsUnderVcs(vcs);
                    for (VirtualFile file : files) {
                        result.add(new FilePathImpl(file));
                    }
                }
            }
            return result.toArray(FilePath[]::new);
        }
    };

    ScopeInfo FILES = new ScopeInfo() {
        @Override
        public String getScopeName(VcsContext dataContext, ActionInfo actionInfo) {
            FilePath[] roots = getRoots(dataContext, actionInfo);
            if (roots == null || roots.length == 0) {
                return VcsLocalize.updateFilesScopeName().get();
            }
            boolean directory = roots[0].isDirectory();
            if (roots.length == 1) {
                return directory ? VcsLocalize.updateDirectoryScopeName().get() : VcsLocalize.updateFileScopeName().get();
            }
            else if (directory) {
                return VcsLocalize.updateDirectoriesScopeName().get();
            }
            else {
                return VcsLocalize.updateFilesScopeName().get();
            }
        }

        @Override
        public boolean filterExistsInVcs() {
            return true;
        }

        @Override
        public FilePath[] getRoots(VcsContext context, ActionInfo actionInfo) {
            return context.getSelectedFilePaths();
        }
    };
}
