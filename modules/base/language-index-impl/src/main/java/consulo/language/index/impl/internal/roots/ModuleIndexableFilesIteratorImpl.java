// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
package consulo.language.index.impl.internal.roots;

import consulo.content.ContentIterator;
import consulo.language.index.impl.internal.localize.IndexingLocalize;
import consulo.language.index.impl.internal.roots.ModuleIndexableFilesIterator;
import consulo.language.index.impl.internal.roots.kind.ModuleRootOrigin;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ModuleIndexableFilesIteratorImpl implements ModuleIndexableFilesIterator {
    private final Module myModule;
    private final List<VirtualFile> myRoots;
    private final boolean myShouldPrintSingleRootInDebugName;

    ModuleIndexableFilesIteratorImpl(Module module, List<VirtualFile> roots, boolean shouldPrintSingleRootInDebugName) {
        myModule = module;
        myRoots = roots;
        myShouldPrintSingleRootInDebugName = shouldPrintSingleRootInDebugName;
    }

    public static Collection<ModuleIndexableFilesIteratorImpl> getModuleIterators(Module module) {
        ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(module.getProject());

        List<VirtualFile> moduleRoots = new ArrayList<>();
        for (VirtualFile contentRoot : rootManager.getContentRoots()) {
            if (module.equals(projectFileIndex.getModuleForFile(contentRoot))) {
                moduleRoots.add(contentRoot);
            }
        }
        if (moduleRoots.isEmpty()) {
            return List.of();
        }

        List<ModuleIndexableFilesIteratorImpl> iterators = new ArrayList<>(moduleRoots.size());
        for (VirtualFile root : moduleRoots) {
            iterators.add(new ModuleIndexableFilesIteratorImpl(module, List.of(root), moduleRoots.size() > 1));
        }
        return iterators;
    }

    @Override
    public String getDebugName() {
        return "Module '" + myModule.getName() + "'" + (myShouldPrintSingleRootInDebugName ? " (" + myRoots.get(0).getName() + ")" : "");
    }

    @Override
    public LocalizeValue getIndexingProgressText() {
        return IndexingLocalize.indexableFilesProviderIndexingModuleName(myModule.getName());
    }

    @Override
    public LocalizeValue getRootsScanningProgressText() {
        return IndexingLocalize.indexableFilesProviderScanningModuleName(myModule.getName());
    }

    @Override
    public ModuleRootOrigin getOrigin() {
        return new ModuleRootOriginImpl(myModule, myRoots);
    }

    @Override
    public boolean iterateFiles(Project project, ContentIterator fileIterator, VirtualFileFilter fileFilter) {
        for (VirtualFile root : myRoots) {
            ModuleRootManager.getInstance(myModule).getFileIndex().iterateContentUnderDirectory(root, fileIterator, fileFilter);
        }
        return true;
    }
}
