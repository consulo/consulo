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

/*
 * @author max
 */
package consulo.ide.impl.idea.ide.projectView.impl.nodes;

import consulo.application.AllIcons;
import consulo.ide.IdeBundle;
import consulo.project.ui.view.internal.node.NamedLibraryElement;
import consulo.project.ui.view.tree.PsiDirectoryNode;
import consulo.ui.ex.tree.PresentationData;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.content.bundle.Sdk;
import consulo.content.library.Library;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiManager;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.module.content.layer.orderEntry.OrderEntryWithTracking;
import consulo.content.base.BinariesOrderRootType;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

import java.util.*;

public class ExternalLibrariesNode extends ProjectViewNode<String> {
    public ExternalLibrariesNode(Project project, ViewSettings viewSettings) {
        super(project, IdeBundle.message("node.projectview.external.libraries"), viewSettings);
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
        ProjectFileIndex index = ProjectRootManager.getInstance(getProject()).getFileIndex();
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
        final List<AbstractTreeNode> children = new ArrayList<AbstractTreeNode>();
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
        Module[] modules = ModuleManager.getInstance(getProject()).getModules();
        Set<Library> processedLibraries = new HashSet<Library>();
        Set<Sdk> processedSdk = new HashSet<Sdk>();
        Set<OrderEntry> processedCustomOrderEntries = new HashSet<OrderEntry>();

        for (Module module : modules) {
            final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
            final OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();
            loop:
            for (final OrderEntry orderEntry : orderEntries) {
                if (orderEntry instanceof LibraryOrderEntry) {
                    final LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) orderEntry;
                    final Library library = libraryOrderEntry.getLibrary();
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

                    final String libraryName = library.getName();
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
                else if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry) {
                    final ModuleExtensionWithSdkOrderEntry sdkOrderEntry = (ModuleExtensionWithSdkOrderEntry) orderEntry;
                    final Sdk sdk = sdkOrderEntry.getSdk();
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
        final LibraryOrderEntry entry,
        final List<AbstractTreeNode> children,
        Project project,
        ProjectViewNode node
    ) {
        final PsiManager psiManager = PsiManager.getInstance(project);
        final VirtualFile[] files = entry.getFiles(BinariesOrderRootType.getInstance());
        for (final VirtualFile file : files) {
            final PsiDirectory psiDir = psiManager.findDirectory(file);
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
        presentation.setPresentableText(IdeBundle.message("node.projectview.external.libraries"));
        presentation.setIcon(AllIcons.Nodes.PpLibFolder);
    }
}
