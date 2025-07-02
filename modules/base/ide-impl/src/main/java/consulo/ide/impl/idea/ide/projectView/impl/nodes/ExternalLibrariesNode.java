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
import consulo.content.base.BinariesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.library.Library;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntryWithTracking;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.internal.node.NamedLibraryElement;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.PsiDirectoryNode;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tree.PresentationData;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author max
 */
public class ExternalLibrariesNode extends ProjectViewNode<String> {
    public ExternalLibrariesNode(Project project, ViewSettings viewSettings) {
        super(project, IdeLocalize.nodeProjectviewExternalLibraries().get(), viewSettings);
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

    @RequiredReadAction
    @Nonnull
    @Override
    @RequiredUIAccess
    public Collection<? extends AbstractTreeNode> getChildren() {
        List<AbstractTreeNode> children = new ArrayList<>();
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
        Module[] modules = ModuleManager.getInstance(getProject()).getModules();
        Set<Library> processedLibraries = new HashSet<>();
        Set<Sdk> processedSdk = new HashSet<>();
        Set<OrderEntry> processedCustomOrderEntries = new HashSet<>();

        for (Module module : modules) {
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
            OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();
            loop:
            for (OrderEntry orderEntry : orderEntries) {
                if (orderEntry instanceof LibraryOrderEntry libraryOrderEntry) {
                    Library library = libraryOrderEntry.getLibrary();
                    if (library == null) {
                        continue;
                    }
                    if (processedLibraries.contains(library)) {
                        continue;
                    }
                    processedLibraries.add(library);

                    if (!hasExternalEntries(fileIndex, libraryOrderEntry)) {
                        continue;
                    }

                    String libraryName = library.getName();
                    if (libraryName == null || libraryName.length() == 0) {
                        addLibraryChildren(libraryOrderEntry, children, getProject(), this);
                    }
                    else {
                        children.add(new NamedLibraryElementNode(
                            getProject(),
                            new NamedLibraryElement(null, libraryOrderEntry),
                            getSettings()
                        ));
                    }
                }
                else if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry sdkOrderEntry) {
                    Sdk sdk = sdkOrderEntry.getSdk();
                    if (sdk != null) {
                        if (processedSdk.contains(sdk)) {
                            continue;
                        }
                        processedSdk.add(sdk);
                        children.add(new NamedLibraryElementNode(
                            getProject(),
                            new NamedLibraryElement(null, sdkOrderEntry),
                            getSettings()
                        ));
                    }
                }
                else if (orderEntry instanceof OrderEntryWithTracking) {
                    if (!orderEntry.isValid()) {
                        continue;
                    }
                    for (OrderEntry processedCustomOrderEntry : processedCustomOrderEntries) {
                        if (processedCustomOrderEntry.isEquivalentTo(orderEntry)) {
                            continue loop;
                        }
                    }
                    processedCustomOrderEntries.add(orderEntry);
                    children.add(new NamedLibraryElementNode(getProject(), new NamedLibraryElement(null, orderEntry), getSettings()));
                }
            }
        }
        return children;
    }

    @RequiredReadAction
    public static void addLibraryChildren(
        LibraryOrderEntry entry,
        List<AbstractTreeNode> children,
        Project project,
        ProjectViewNode node
    ) {
        PsiManager psiManager = PsiManager.getInstance(project);
        VirtualFile[] files = entry.getFiles(BinariesOrderRootType.getInstance());
        for (VirtualFile file : files) {
            PsiDirectory psiDir = psiManager.findDirectory(file);
            if (psiDir == null) {
                continue;
            }
            children.add(new PsiDirectoryNode(project, psiDir, node.getSettings()));
        }
    }

    private static boolean hasExternalEntries(ProjectFileIndex index, LibraryOrderEntry orderEntry) {
        for (VirtualFile file : LibraryGroupNode.getLibraryRoots(orderEntry)) {
            if (!index.isInContent(VirtualFilePathUtil.getLocalFile(file))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void update(PresentationData presentation) {
        presentation.setPresentableText(IdeLocalize.nodeProjectviewExternalLibraries());
        presentation.setIcon(PlatformIconGroup.nodesPplibfolder());
    }
}
