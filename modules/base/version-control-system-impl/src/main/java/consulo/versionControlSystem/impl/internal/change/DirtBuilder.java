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
package consulo.versionControlSystem.impl.internal.change;

import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.VcsDirtyScopeBuilder;
import consulo.versionControlSystem.change.VcsModifiableDirtyScope;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.util.VcsUtil;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirtBuilder {
    private boolean isEverythingDirty;
    private Map<AbstractVcs, VcsDirtyScopeBuilder> scopesByVcs = new HashMap<>();

    public boolean isEmpty() {
        return !isEverythingDirty && scopesByVcs.isEmpty();
    }

    public void markEverythingDirty() {
        isEverythingDirty = true;
        scopesByVcs.clear();
    }

    public boolean isEverythingDirty() {
        return isEverythingDirty;
    }

    public boolean addDirtyFiles(VcsRoot vcsRoot, Collection<FilePath> files, Collection<FilePath> dirs) {
        if (isEverythingDirty) {
            return true;
        }

        var vcs = vcsRoot.getVcs();
        var root = vcsRoot.getPath();
        if (vcs != null) {
            var scope = scopesByVcs.computeIfAbsent(vcs, this::createDirtyScope);
            for (var filePath : files) {
                scope.addDirtyPathFast(root, filePath, false);
            }
            for (var filePath : dirs) {
                scope.addDirtyPathFast(root, filePath, true);
            }
        }
        return !scopesByVcs.isEmpty();
    }

    @Nonnull
    public List<VcsModifiableDirtyScope> buildScopes(Project project) {
        Collection<VcsDirtyScopeBuilder> scopes;

        if (isEverythingDirty) {
            var allScopes = new HashMap<AbstractVcs, VcsDirtyScopeBuilder>();
            for (var root : ProjectLevelVcsManager.getInstance(project).getAllVcsRoots()) {
                var vcs = root.getVcs();
                var path = root.getPath();
                if (vcs != null) {
                    var scope = allScopes.computeIfAbsent(vcs, this::createDirtyScope);
                    scope.markEverythingDirty();
                    scope.addDirtyPathFast(path, VcsUtil.getFilePath(path), true);
                }
            }
            scopes = allScopes.values();
        }
        else {
            scopes = scopesByVcs.values();
        }
        return ContainerUtil.map(scopes, VcsDirtyScopeBuilder::pack);
    }

    public boolean isFileDirty(FilePath filePath) {
        return isEverythingDirty || ContainerUtil.any(scopesByVcs.values(), it -> it.belongsTo(filePath));
    }

    @Nonnull
    private VcsDirtyScopeBuilder createDirtyScope(AbstractVcs vcs) {
        VcsDirtyScopeBuilder scope = vcs.createDirtyScope();
        if (scope != null) {
            return scope;
        }
        return new VcsDirtyScopeImpl(vcs);
    }
}
