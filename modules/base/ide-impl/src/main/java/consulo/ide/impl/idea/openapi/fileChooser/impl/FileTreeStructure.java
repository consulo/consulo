/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.fileChooser.impl;

import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.fileChooser.FileElement;
import consulo.ide.impl.idea.openapi.fileChooser.ex.FileNodeDescriptor;
import consulo.ide.impl.idea.openapi.fileChooser.ex.RootFileElement;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Yura Cangea
 */
public class FileTreeStructure extends AbstractTreeStructure {
    private static final Logger LOG = Logger.getInstance(FileTreeStructure.class);

    private final RootFileElement myRootElement;
    private final FileChooserDescriptor myChooserDescriptor;
    private boolean myShowHidden;
    private final Project myProject;

    public FileTreeStructure(Project project, FileChooserDescriptor chooserDescriptor) {
        myProject = project;
        List<VirtualFile> rootFiles = chooserDescriptor.getRoots();
        String name = rootFiles.size() == 1 && rootFiles.get(0) != null ? rootFiles.get(0).getPresentableUrl() : chooserDescriptor.getTitle();
        myRootElement = new RootFileElement(rootFiles, name, chooserDescriptor.isShowFileSystemRoots());
        myChooserDescriptor = chooserDescriptor;
        myShowHidden = myChooserDescriptor.isShowHiddenFiles();
    }

    @Override
    public boolean isToBuildChildrenInBackground(@Nonnull final Object element) {
        return true;
    }

    public final boolean areHiddensShown() {
        return myShowHidden;
    }

    public final void showHiddens(final boolean showHidden) {
        myShowHidden = showHidden;
    }

    @Nonnull
    @Override
    public final Object getRootElement() {
        return myRootElement;
    }

    @Nonnull
    @Override
    public Object[] getChildElements(@Nonnull Object nodeElement) {
        if (!(nodeElement instanceof FileElement)) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        FileElement element = (FileElement) nodeElement;
        VirtualFile file = element.getFile();

        if (file == null || !file.isValid()) {
            if (element == myRootElement) {
                return myRootElement.getChildren();
            }
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        VirtualFile[] children = null;

        if (element.isArchive() && myChooserDescriptor.isChooseJarContents()) {
            String path = file.getPath();
            if (!(file.getFileSystem() instanceof ArchiveFileSystem)) {
                file = ((ArchiveFileType) file.getFileType()).getFileSystem().findLocalVirtualFileByPath(path);
            }
            if (file != null) {
                children = file.getChildren();
            }
        }
        else {
            children = file.getChildren();
        }

        if (children == null) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        Set<FileElement> childrenSet = new HashSet<>();
        for (VirtualFile child : children) {
            if (myChooserDescriptor.isFileVisible(child, myShowHidden)) {
                final FileElement childElement = new FileElement(child, child.getName());
                childElement.setParent(element);
                childrenSet.add(childElement);
            }
        }
        return ArrayUtil.toObjectArray(childrenSet);
    }


    @Override
    @Nullable
    public Object getParentElement(@Nonnull Object element) {
        if (element instanceof FileElement fileElement) {
            final VirtualFile elementFile = getValidFile(fileElement);
            if (elementFile != null && myRootElement.getFile() != null && myRootElement.getFile().equals(elementFile)) {
                return null;
            }

            final VirtualFile parentElementFile = getValidFile(fileElement.getParent());

            if (elementFile != null && parentElementFile != null) {
                final VirtualFile parentFile = elementFile.getParent();
                if (parentElementFile.equals(parentFile)) {
                    return fileElement.getParent();
                }
            }

            VirtualFile file = fileElement.getFile();
            if (file == null) {
                return null;
            }
            VirtualFile parent = file.getParent();
            if (parent != null && parent.getFileSystem() instanceof ArchiveFileSystem && parent.getParent() == null) {
                // parent of jar contents should be local jar file
                String localPath = parent.getPath().substring(0,
                    parent.getPath().length() - ArchiveFileSystem.ARCHIVE_SEPARATOR.length());
                parent = LocalFileSystem.getInstance().findFileByPath(localPath);
            }

            if (parent != null && parent.isValid() && parent.equals(myRootElement.getFile())) {
                return myRootElement;
            }

            if (parent == null) {
                return myRootElement;
            }
            return new FileElement(parent, parent.getName());
        }
        return null;
    }

    @Nullable
    private static VirtualFile getValidFile(FileElement element) {
        if (element == null) {
            return null;
        }
        final VirtualFile file = element.getFile();
        return file != null && file.isValid() ? file : null;
    }

    @Override
    public final void commit() {
    }

    @Override
    public final boolean hasSomethingToCommit() {
        return false;
    }

    @Override
    @Nonnull
    public NodeDescriptor createDescriptor(Object element, NodeDescriptor parentDescriptor) {
        LOG.assertTrue(element instanceof FileElement, element.getClass().getName());
        VirtualFile file = ((FileElement) element).getFile();
        Image icon = file == null ? null : myChooserDescriptor.getIcon(file);
        String name = file == null ? null : myChooserDescriptor.getName(file);
        String comment = file == null ? null : myChooserDescriptor.getComment(file);

        return new FileNodeDescriptor(myProject, (FileElement) element, parentDescriptor, icon, name, comment);
    }
}
