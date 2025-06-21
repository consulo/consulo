/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import consulo.content.OrderRootType;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.internal.LibraryEx;
import consulo.content.internal.LibraryKindRegistry;
import consulo.content.library.Library;
import consulo.content.library.LibraryType;
import consulo.content.library.PersistentLibraryKind;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.internal.ProjectSettingsService;
import consulo.project.ui.view.internal.node.LibraryGroupElement;
import consulo.project.ui.view.internal.node.NamedLibraryElement;
import consulo.project.ui.view.tree.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tree.PresentationData;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class LibraryGroupNode extends ProjectViewNode<LibraryGroupElement> {
    public LibraryGroupNode(Project project, LibraryGroupElement value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    public LibraryGroupNode(Project project, Object value, ViewSettings viewSettings) {
        this(project, (LibraryGroupElement) value, viewSettings);
    }

    @RequiredReadAction
    @Override
    @Nonnull
    public Collection<AbstractTreeNode> getChildren() {
        Module module = getValue().getModule();
        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        List<AbstractTreeNode> children = new ArrayList<>();
        OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();
        for (OrderEntry orderEntry : orderEntries) {
            if (orderEntry instanceof LibraryOrderEntry libraryOrderEntry) {
                Library library = libraryOrderEntry.getLibrary();
                if (library == null) {
                    continue;
                }
                String libraryName = library.getName();
                if (libraryName == null || libraryName.length() == 0) {
                    addLibraryChildren(libraryOrderEntry, children, getProject(), this);
                }
                else {
                    children.add(new NamedLibraryElementNode(
                        getProject(),
                        new NamedLibraryElement(module, libraryOrderEntry),
                        getSettings()
                    ));
                }
            }
            else if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry sdkOrderEntry) {
                Sdk jdk = sdkOrderEntry.getSdk();
                if (jdk != null) {
                    children.add(new NamedLibraryElementNode(getProject(), new NamedLibraryElement(module, sdkOrderEntry), getSettings()));
                }
            }
        }
        return children;
    }

    @RequiredReadAction
    public static void addLibraryChildren(OrderEntry entry, List<AbstractTreeNode> children, Project project, ProjectViewNode node) {
        PsiManager psiManager = PsiManager.getInstance(project);
        VirtualFile[] files = entry instanceof LibraryOrderEntry libraryOrderEntry
            ? getLibraryRoots(libraryOrderEntry)
            : entry.getFiles(BinariesOrderRootType.getInstance());
        for (VirtualFile file : files) {
            if (!file.isValid()) {
                continue;
            }
            if (file.isDirectory()) {
                PsiDirectory psiDir = psiManager.findDirectory(file);
                if (psiDir == null) {
                    continue;
                }
                children.add(new PsiDirectoryNode(project, psiDir, node.getSettings()));
            }
            else {
                PsiFile psiFile = psiManager.findFile(file);
                if (psiFile == null) {
                    continue;
                }
                children.add(new PsiFileNode(project, psiFile, node.getSettings()));
            }
        }
    }


    @Override
    public String getTestPresentation() {
        return "Libraries";
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

    @Override
    public void update(PresentationData presentation) {
        presentation.setPresentableText(IdeLocalize.nodeProjectviewLibraries());
        presentation.setIcon(PlatformIconGroup.nodesPplib());
    }

    @Override
    public boolean canNavigate() {
        return ProjectSettingsService.getInstance(myProject).canOpenModuleLibrarySettings();
    }

    @Override
    @RequiredUIAccess
    public void navigate(boolean requestFocus) {
        Module module = getValue().getModule();
        ProjectSettingsService.getInstance(myProject).openModuleLibrarySettings(module);
    }

    @Nonnull
    static VirtualFile[] getLibraryRoots(@Nonnull LibraryOrderEntry orderEntry) {
        Library library = orderEntry.getLibrary();
        if (library == null) {
            return VirtualFile.EMPTY_ARRAY;
        }
        OrderRootType[] rootTypes = LibraryType.getDefaultExternalRootTypes();
        if (library instanceof LibraryEx) {
            if (library.isDisposed()) {
                return VirtualFile.EMPTY_ARRAY;
            }
            PersistentLibraryKind<?> libKind = library.getKind();
            if (libKind != null) {
                LibraryType<?> type = LibraryKindRegistry.getInstance().findLibraryTypeByKindId(libKind.getKindId());
                if (type != null) {
                    rootTypes = type.getExternalRootTypes();
                }
            }
        }
        List<VirtualFile> files = new ArrayList<>();
        for (OrderRootType rootType : rootTypes) {
            files.addAll(Arrays.asList(library.getFiles(rootType)));
        }
        return VfsUtilCore.toVirtualFileArray(files);
    }
}
