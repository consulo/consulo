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
package consulo.ide.impl.idea.ide.projectView.impl.nodes;

import consulo.annotation.access.RequiredReadAction;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.view.tree.PackageNodeUtil;
import consulo.ui.ex.tree.PresentationData;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PackageViewLibrariesNode extends ProjectViewNode<LibrariesElement> {
    public PackageViewLibrariesNode(Project project, Module module, ViewSettings viewSettings) {
        super(project, new LibrariesElement(module, project), viewSettings);
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
        ProjectFileIndex index = ProjectRootManager.getInstance(getProject()).getFileIndex();
        //noinspection SimplifiableIfStatement
        if (!index.isInLibrarySource(file) && !index.isInLibraryClasses(file)) {
            return false;
        }

        return someChildContainsFile(file, false);
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public Collection<AbstractTreeNode> getChildren() {
        List<VirtualFile> roots = new ArrayList<>();
        Module myModule = getValue().module();
        if (myModule == null) {
            Module[] modules = ModuleManager.getInstance(getProject()).getModules();
            for (Module module : modules) {
                addModuleLibraryRoots(ModuleRootManager.getInstance(module), roots);
            }
        }
        else {
            addModuleLibraryRoots(ModuleRootManager.getInstance(myModule), roots);
        }
        return PackageNodeUtil.createPackageViewChildrenOnFiles(roots, getProject(), getSettings(), null, true);
    }


    @Override
    public boolean someChildContainsFile(VirtualFile file) {
        ProjectFileIndex index = ProjectRootManager.getInstance(getProject()).getFileIndex();
        //noinspection SimplifiableIfStatement
        if (!index.isInLibrarySource(file) && !index.isInLibraryClasses(file)) {
            return false;
        }
        return super.someChildContainsFile(file);
    }

    private static void addModuleLibraryRoots(ModuleRootManager moduleRootManager, List<VirtualFile> roots) {
        VirtualFile[] files = moduleRootManager.orderEntries().withoutModuleSourceEntries().withoutDepModules().classes().getRoots();
        for (VirtualFile file : files) {
            if (file.getFileSystem() instanceof ArchiveFileSystem && file.getParent() != null) {
                // skip entries inside jars
                continue;
            }
            roots.add(file);
        }
    }

    @Override
    public void update(PresentationData presentation) {
        presentation.setPresentableText(IdeLocalize.nodeProjectviewLibraries());
        presentation.setIcon(PlatformIconGroup.nodesPplibfolder());
    }

    @Override
    public String getTestPresentation() {
        return "Libraries";
    }

    @Override
    public boolean shouldUpdateData() {
        return true;
    }

    @Override
    public int getWeight() {
        return 60;
    }
}
