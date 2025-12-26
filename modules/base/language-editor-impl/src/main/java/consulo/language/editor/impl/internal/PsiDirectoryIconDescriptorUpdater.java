/*
 * Copyright 2013-2017 consulo.io
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
package consulo.language.editor.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.content.ContentFolderTypeProvider;
import consulo.language.content.ContentFoldersSupportUtil;
import consulo.language.content.PackageBasedContentFolderTypeProvider;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdater;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiPackage;
import consulo.language.psi.PsiPackageManager;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.internal.ModuleIconService;
import consulo.module.content.layer.ContentFolder;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 09-Jan-17
 */
@ExtensionImpl(id = "directory")
public class PsiDirectoryIconDescriptorUpdater implements IconDescriptorUpdater {
    private final ProjectFileIndex myProjectFileIndex;
    private final ProjectRootManager myProjectRootManager;
    private final PsiPackageManager myPsiPackageManager;
    private final Project myProject;
    private final ModuleIconService myModuleIconService;

    @Inject
    public PsiDirectoryIconDescriptorUpdater(Project project,
                                             ProjectFileIndex projectFileIndex,
                                             ProjectRootManager projectRootManager,
                                             PsiPackageManager psiPackageManager, ModuleIconService moduleIconService) {
        myProject = project;
        myProjectFileIndex = projectFileIndex;
        myProjectRootManager = projectRootManager;
        myPsiPackageManager = psiPackageManager;
        myModuleIconService = moduleIconService;
    }

    @RequiredReadAction
    @Override
    public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags) {
        if (element instanceof PsiDirectory) {
            PsiDirectory psiDirectory = (PsiDirectory) element;
            VirtualFile virtualFile = psiDirectory.getVirtualFile();

            Image symbolIcon;
            if (virtualFile.getFileSystem() instanceof ArchiveFileSystem) {
                if (virtualFile.getParent() == null) {
                    symbolIcon = PlatformIconGroup.filetypesArchive();
                }
                else {
                    PsiPackage psiPackage = myPsiPackageManager.findAnyPackage(virtualFile);
                    symbolIcon = psiPackage != null ? PlatformIconGroup.nodesPackage() : PlatformIconGroup.nodesTreeclosed();
                }
            }
            else if (ProjectRootsUtil.isModuleContentRoot(myProjectFileIndex, virtualFile)) {
                Module module = myProjectFileIndex.getModuleForFile(virtualFile);
                symbolIcon = module != null ? myModuleIconService.getIcon(module) : PlatformIconGroup.nodesModule();
            }
            else {
                boolean ignored = myProjectRootManager.getFileIndex().isExcluded(virtualFile);
                if (ignored) {
                    symbolIcon = PlatformIconGroup.modulesExcluderoot();
                }
                else {
                    ContentFolder contentFolder = ProjectRootsUtil.findContentFolderForDirectory(myProjectFileIndex, virtualFile);
                    if (contentFolder != null) {
                        symbolIcon = ContentFoldersSupportUtil.getContentFolderIcon(contentFolder.getType(), contentFolder.getProperties());
                    }
                    else {
                        ContentFolderTypeProvider contentFolderTypeForFile = myProjectFileIndex.getContentFolderTypeForFile(virtualFile);
                        if (contentFolderTypeForFile != null) {
                            if (contentFolderTypeForFile instanceof PackageBasedContentFolderTypeProvider p) {
                                symbolIcon = p.getChildDirectoryIcon(psiDirectory, myPsiPackageManager);
                            }
                            else {
                                symbolIcon = contentFolderTypeForFile.getChildDirectoryIcon(virtualFile, myProject);
                            }
                        }
                        else {
                            symbolIcon = PlatformIconGroup.nodesTreeclosed();
                        }
                    }
                }
            }

            if (symbolIcon != null) {
                iconDescriptor.setMainIcon(symbolIcon);
            }

            if (virtualFile.is(VFileProperty.SYMLINK)) {
                iconDescriptor.addLayerIcon(PlatformIconGroup.nodesSymlink());
            }
        }
        else if (element instanceof PsiPackage) {
            iconDescriptor.setMainIcon(PlatformIconGroup.nodesPackage());
        }
    }
}
