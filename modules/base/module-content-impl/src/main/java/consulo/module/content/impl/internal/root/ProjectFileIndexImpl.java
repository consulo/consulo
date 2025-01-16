/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package consulo.module.content.impl.internal.root;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessRule;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.ContentIterator;
import consulo.content.ResourceLikeContentFolderTypeProvider;
import consulo.content.TestLikeContentFolderTypeProvider;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.DirectoryIndex;
import consulo.module.content.DirectoryInfo;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.internal.FileIndexBase;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileDelegate;
import consulo.virtualFileSystem.VirtualFileFilter;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.util.*;

@Singleton
@ServiceImpl
public class ProjectFileIndexImpl extends FileIndexBase implements ProjectFileIndex {
    private static final Logger LOG = Logger.getInstance(ProjectFileIndexImpl.class);
    private final Project myProject;

    @Inject
    public ProjectFileIndexImpl(@Nonnull Project project, @Nonnull Provider<DirectoryIndex> directoryIndex, @Nonnull FileTypeRegistry fileTypeManager) {
        super(directoryIndex, fileTypeManager);
        myProject = project;
    }

    @Override
    public boolean iterateContent(@Nonnull ContentIterator processor, @Nullable VirtualFileFilter filter) {
        Module[] modules = AccessRule.read(() -> ModuleManager.getInstance(myProject).getModules());
        for (final Module module : modules) {
            for (VirtualFile contentRoot : getRootsToIterate(module)) {
                if (!iterateContentUnderDirectory(contentRoot, processor, filter)) {
                    return false;
                }
            }
        }
        return true;
    }

    private Set<VirtualFile> getRootsToIterate(final Module module) {
        return AccessRule.read(() -> {
            if (module.isDisposed()) {
                return Collections.emptySet();
            }

            Set<VirtualFile> result = new LinkedHashSet<>();
            for (VirtualFile[] roots : getModuleContentAndSourceRoots(module)) {
                for (VirtualFile root : roots) {
                    DirectoryInfo info = getInfoForFileOrDirectory(root);
                    if (!info.isInProject(root)) {
                        continue; // is excluded or ignored
                    }
                    if (!module.equals(info.getModule())) {
                        continue; // maybe 2 modules have the same content root?
                    }

                    VirtualFile parent = root.getParent();
                    if (parent != null) {
                        DirectoryInfo parentInfo = getInfoForFileOrDirectory(parent);
                        if (isFileInContent(parent, parentInfo)) {
                            continue;
                        }
                    }
                    result.add(root);
                }
            }

            return result;
        });
    }

    @Override
    public boolean isExcluded(@Nonnull VirtualFile file) {
        DirectoryInfo info = getInfoForFileOrDirectory(file);
        return info.isIgnored() || info.isExcluded(file);
    }

    @Override
    public boolean isUnderIgnored(@Nonnull VirtualFile file) {
        return getInfoForFileOrDirectory(file).isIgnored();
    }

    @Override
    public Module getModuleForFile(@Nonnull VirtualFile file) {
        return getModuleForFile(file, true);
    }

    @Nullable
    @Override
    public Module getModuleForFile(@Nonnull VirtualFile file, boolean honorExclusion) {
        if (file instanceof VirtualFileDelegate) {
            file = ((VirtualFileDelegate) file).getDelegate();
        }
        DirectoryInfo info = getInfoForFileOrDirectory(file);
        if (info.isInProject(file) || !honorExclusion && info.isExcluded(file)) {
            return info.getModule();
        }
        return null;
    }

    @Nullable
    @Override
    public ContentFolder getContentFolder(@Nonnull VirtualFile file) {
        DirectoryInfo info = getInfoForFileOrDirectory(file);
        return info.getContentFolder();
    }

    @Override
    @Nonnull
    public List<OrderEntry> getOrderEntriesForFile(@Nonnull VirtualFile file) {
        return Arrays.asList(myDirectoryIndexProvider.get().getOrderEntries(getInfoForFileOrDirectory(file)));
    }

    @Override
    public VirtualFile getClassRootForFile(@Nonnull VirtualFile file) {
        return getClassRootForFile(file, getInfoForFileOrDirectory(file));
    }

    @Nullable
    public static VirtualFile getClassRootForFile(@Nonnull VirtualFile file, DirectoryInfo info) {
        return info.isInProject(file) ? info.getLibraryClassRoot() : null;
    }

    @Override
    public VirtualFile getSourceRootForFile(@Nonnull VirtualFile file) {
        return getSourceRootForFile(file, getInfoForFileOrDirectory(file));
    }

    @Nullable
    public static VirtualFile getSourceRootForFile(@Nonnull VirtualFile file, DirectoryInfo info) {
        return info.isInProject(file) ? info.getSourceRoot() : null;
    }

    @Override
    public VirtualFile getContentRootForFile(@Nonnull VirtualFile file) {
        return getContentRootForFile(file, true);
    }

    @Override
    public VirtualFile getContentRootForFile(@Nonnull VirtualFile file, final boolean honorExclusion) {
        return getContentRootForFile(getInfoForFileOrDirectory(file), file, honorExclusion);
    }

    @Nullable
    public static VirtualFile getContentRootForFile(DirectoryInfo info, @Nonnull VirtualFile file, boolean honorExclusion) {
        if (info.isInProject(file) || !honorExclusion && info.isExcluded(file)) {
            return info.getContentRoot();
        }
        return null;
    }

    @Override
    public String getPackageNameByDirectory(@Nonnull VirtualFile dir) {
        if (!dir.isDirectory()) {
            LOG.error(dir.getPresentableUrl());
        }
        return myDirectoryIndexProvider.get().getPackageName(dir);
    }

    @Override
    public boolean isLibraryClassFile(@Nonnull VirtualFile file) {
        if (file.isDirectory()) {
            return false;
        }
        DirectoryInfo parentInfo = getInfoForFileOrDirectory(file);
        return parentInfo.isInProject(file) && parentInfo.hasLibraryClassRoot();
    }

    @Override
    public boolean isInSource(@Nonnull VirtualFile fileOrDir) {
        DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
        return info.isInModuleSource(fileOrDir) || info.isInLibrarySource(fileOrDir);
    }

    @Override
    public boolean isInResource(@Nonnull VirtualFile fileOrDir) {
        DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
        if (!info.isInModuleSource(fileOrDir)) {
            return false;
        }
        
        ContentFolderTypeProvider type = myDirectoryIndexProvider.get().getContentFolderType(fileOrDir, info);
        return type instanceof ResourceLikeContentFolderTypeProvider && !(type instanceof TestLikeContentFolderTypeProvider);
    }

    @Override
    public boolean isInTestResource(@Nonnull VirtualFile fileOrDir) {
        DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
        if (!info.isInModuleSource(fileOrDir)) {
            return false;
        }

        ContentFolderTypeProvider type = myDirectoryIndexProvider.get().getContentFolderType(fileOrDir, info);
        return type instanceof ResourceLikeContentFolderTypeProvider && type instanceof TestLikeContentFolderTypeProvider;
    }

    @Override
    public boolean isInLibraryClasses(@Nonnull VirtualFile fileOrDir) {
        DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
        return info.isInProject(fileOrDir) && info.hasLibraryClassRoot();
    }

    @Override
    public boolean isInLibrarySource(@Nonnull VirtualFile fileOrDir) {
        DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
        return info.isInProject(fileOrDir) && info.isInLibrarySource(fileOrDir);
    }

    // a slightly faster implementation then the default one
    @Override
    public boolean isInLibrary(@Nonnull VirtualFile fileOrDir) {
        DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
        return info.isInProject(fileOrDir) && (info.hasLibraryClassRoot() || info.isInLibrarySource(fileOrDir));
    }

    @Override
    public boolean isIgnored(@Nonnull VirtualFile file) {
        return isExcluded(file);
    }

    @Override
    public boolean isInContent(@Nonnull VirtualFile fileOrDir) {
        return isFileInContent(fileOrDir, getInfoForFileOrDirectory(fileOrDir));
    }

    public static boolean isFileInContent(@Nonnull VirtualFile fileOrDir, @Nonnull DirectoryInfo info) {
        return info.isInProject(fileOrDir) && info.getModule() != null;
    }

    @Override
    public boolean isInSourceContent(@Nonnull VirtualFile fileOrDir) {
        return getInfoForFileOrDirectory(fileOrDir).isInModuleSource(fileOrDir);
    }

    @Override
    public boolean isInTestSourceContent(@Nonnull VirtualFile fileOrDir) {
        DirectoryInfo info = getInfoForFileOrDirectory(fileOrDir);
        return info.isInModuleSource(fileOrDir) &&
            myDirectoryIndexProvider.get().getContentFolderType(fileOrDir, info) instanceof TestLikeContentFolderTypeProvider;
    }

    @Nullable
    @Override
    public ContentFolderTypeProvider getContentFolderTypeForFile(@Nonnull VirtualFile file) {
        DirectoryInfo info = getInfoForFileOrDirectory(file);
        return myDirectoryIndexProvider.get().getContentFolderType(file, info);
    }

    @Override
    protected boolean isScopeDisposed() {
        return myProject.isDisposed();
    }
}
