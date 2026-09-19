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

import consulo.language.file.FileTypeManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSetBase;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * @author lesya
 */
public class FileTreeNode extends FileOrDirectoryTreeNode {
    public FileTreeNode(String path, SimpleTextAttributes invalidAttributes, Project project, String parentPath) {
        super(path, invalidAttributes, project, parentPath);
    }

    @Override
    public Image getIcon() {
        if (myFile.isDirectory()) {
            return PlatformIconGroup.nodesTreeclosed();
        }
        return FileTypeManager.getInstance().getFileTypeByFileName(myFile.getName()).getIcon();
    }

    @Override
    protected boolean acceptFilter(Pair<PackageSetBase, NamedScopesHolder> filter, boolean showOnlyFilteredItems) {
        try {
            VirtualFilePointer filePointer = getFilePointer();
            if (!filePointer.isValid()) {
                return false;
            }
            VirtualFile file = filePointer.getFile();
            if (file != null && file.isValid() && filter.first.contains(file, myProject, filter.second)) {
                applyFilter(true);
                return true;
            }
        }
        catch (Throwable e) {
            // TODO: catch and ignore exceptions: see to FilePatternPackageSet
            // sometimes for new file DirectoryFileIndex.getContentRootForFile() return random path
        }
        return false;
    }


    @Override
    public Collection<VirtualFile> getVirtualFiles() {
        VirtualFile virtualFile = getFilePointer().getFile();
        return virtualFile == null ? List.of() : List.of(virtualFile);
    }


    @Override
    public Collection<File> getFiles() {
        return getFilePointer().getFile() == null ? List.of(myFile) : List.of();
    }

    @Override
    protected int getItemsCount() {
        return 1;
    }

    @Override
    protected boolean showStatistics() {
        return false;
    }
}
