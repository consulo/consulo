/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import consulo.application.util.registry.Registry;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.ProjectCoreUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsDirectoryMapping;
import consulo.versionControlSystem.internal.DefaultVcsRootPolicy;
import consulo.versionControlSystem.internal.VcsMapping;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class ModuleDefaultVcsRootPolicy extends DefaultVcsRootPolicy {
    private final Project myProject;
    private final VirtualFile myBaseDir;
    private final ModuleManager myModuleManager;

    @Inject
    public ModuleDefaultVcsRootPolicy(Project project, ModuleManager moduleManager) {
        myProject = project;
        myBaseDir = project.getBaseDir();
        myModuleManager = moduleManager;
    }

    @Override
    @Nonnull
    public Collection<VirtualFile> getDefaultVcsRoots(@Nonnull VcsMapping mappingList, @Nonnull String vcsName) {
        Set<VirtualFile> result = new HashSet<>();
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(myProject);
        if (myBaseDir != null && vcsName.equals(mappingList.getVcsFor(myBaseDir))) {
            AbstractVcs vcsFor = vcsManager.getVcsFor(myBaseDir);
            if (vcsFor != null && vcsName.equals(vcsFor.getName())) {
                result.add(myBaseDir);
            }
        }
        if (ProjectCoreUtil.isDirectoryBased(myProject) && myBaseDir != null) {
            VirtualFile ideaDir = ProjectCoreUtil.getDirectoryStoreFile(myProject);
            if (ideaDir != null) {
                AbstractVcs vcsFor = vcsManager.getVcsFor(ideaDir);
                if (vcsFor != null && vcsName.equals(vcsFor.getName())) {
                    result.add(ideaDir);
                }
            }
        }
        // assertion for read access inside
        Module[] modules = myModuleManager.getModules();
        for (Module module : modules) {
            VirtualFile[] files = ModuleRootManager.getInstance(module).getContentRoots();
            for (VirtualFile file : files) {
                // if we're currently processing moduleAdded notification, getModuleForFile() will return null, so we pass the module
                // explicitly (we know it anyway)
                VcsDirectoryMapping mapping = mappingList.getMappingFor(file, module);
                String mappingVcs = mapping != null ? mapping.getVcs() : null;
                if (vcsName.equals(mappingVcs) && file.isDirectory()) {
                    result.add(file);
                }
            }
        }
        return result;
    }

    @Override
    public boolean matchesDefaultMapping(@Nonnull VirtualFile file, Object matchContext) {
        return matchContext != null || myBaseDir != null && VirtualFileUtil.isAncestor(myBaseDir, file, false);
    }

    @Override
    @Nullable
    public Object getMatchContext(VirtualFile file) {
        return ModuleUtilCore.findModuleForFile(file, myProject);
    }

    @Override
    @Nullable
    public VirtualFile getVcsRootFor(@Nonnull VirtualFile file) {
        if (myBaseDir != null && VirtualFileUtil.isAncestor(myBaseDir, file, false)) {
            return myBaseDir;
        }

        VirtualFile contentRoot = ProjectRootManager.getInstance(myProject).getFileIndex().getContentRootForFile(file, Registry.is("ide.hide.excluded.files"));
        if (contentRoot != null) {
            if (contentRoot.isDirectory()) {
                return contentRoot;
            }
            return contentRoot.getParent();
        }
        if (ProjectCoreUtil.isDirectoryBased(myProject)) {
            VirtualFile ideaDir = ProjectCoreUtil.getDirectoryStoreFile(myProject);
            if (ideaDir != null && VirtualFileUtil.isAncestor(ideaDir, file, false)) {
                return ideaDir;
            }
        }
        return null;
    }
}
