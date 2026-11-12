// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
package consulo.versionControlSystem.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ReadAction;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.Project;
import consulo.project.ProjectCoreUtil;
import consulo.versionControlSystem.internal.DefaultVcsRootPolicy;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The default VCS roots of a project: the base directory, the {@code .idea} directory, and every module content root.
 */
@Singleton
@ServiceImpl
public class ModuleDefaultVcsRootPolicy extends DefaultVcsRootPolicy {
    @Inject
    public ModuleDefaultVcsRootPolicy(Project project) {
        super(project);
        project.getMessageBus().connect().subscribe(ModuleRootListener.class, new MyRootsListener());
    }

    @Override
    public Collection<VirtualFile> getDefaultVcsRoots() {
        Set<VirtualFile> result = new LinkedHashSet<>(findBaseDirectories());
        result.addAll(ReadAction.compute(this::collectWorkspaceRoots));
        return result;
    }

    /**
     * The base directory and its {@code .idea} directory, when the VFS knows them.
     */
    private List<VirtualFile> findBaseDirectories() {
        VirtualFile baseDir = myProject.getBaseDir();
        if (baseDir == null) {
            return List.of();
        }
        VirtualFile ideaDir = ProjectCoreUtil.isDirectoryBased(myProject) ? ProjectCoreUtil.getDirectoryStoreFile(myProject) : null;
        if (ideaDir == null) {
            return List.of(baseDir);
        }
        return List.of(baseDir, ideaDir);
    }

    /**
     * Every module content root. Local directories only.
     */
    private Set<VirtualFile> collectWorkspaceRoots() {
        Set<VirtualFile> result = new LinkedHashSet<>();
        if (myProject.isDisposed()) {
            return result;
        }
        for (Module module : ModuleManager.getInstance(myProject).getModules()) {
            for (VirtualFile file : ModuleRootManager.getInstance(module).getContentRoots()) {
                if (file.isInLocalLikeFileSystem() && file.isDirectory()) {
                    result.add(file);
                }
            }
        }
        return result;
    }

    private class MyRootsListener implements ModuleRootListener {
        @Override
        public void rootsChanged(ModuleRootEvent event) {
            scheduleMappedRootsUpdate();
        }
    }
}
