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
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import consulo.roots.OrderEntryWithTracking;
import consulo.roots.types.BinariesOrderRootType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.util.PathUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import consulo.annotations.RequiredDispatchThread;
import consulo.annotations.RequiredReadAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ExternalLibrariesNode extends ProjectViewNode<String> {
  public ExternalLibrariesNode(Project project, ViewSettings viewSettings) {
    super(project, IdeBundle.message("node.projectview.external.libraries"), viewSettings);
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    ProjectFileIndex index = ProjectRootManager.getInstance(getProject()).getFileIndex();
    if (!index.isInLibrarySource(file) && !index.isInLibraryClasses(file)) return false;

    return someChildContainsFile(file, false);
  }

  @NotNull
  @Override
  @RequiredDispatchThread
  public Collection<? extends AbstractTreeNode> getChildren() {
    final List<AbstractTreeNode> children = new ArrayList<AbstractTreeNode>();
    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
    Module[] modules = ModuleManager.getInstance(getProject()).getModules();
    Set<Library> processedLibraries = new THashSet<Library>();
    Set<Sdk> processedSdk = new THashSet<Sdk>();
    Set<OrderEntry> processedCustomOrderEntries = new THashSet<OrderEntry>();

    for (Module module : modules) {
      final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
      final OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();
      loop:for (final OrderEntry orderEntry : orderEntries) {
        if (orderEntry instanceof LibraryOrderEntry) {
          final LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)orderEntry;
          final Library library = libraryOrderEntry.getLibrary();
          if (library == null) continue;
          if (processedLibraries.contains(library)) continue;
          processedLibraries.add(library);

          if (!hasExternalEntries(fileIndex, libraryOrderEntry)) continue;

          final String libraryName = library.getName();
          if (libraryName == null || libraryName.length() == 0) {
            addLibraryChildren(libraryOrderEntry, children, getProject(), this);
          }
          else {
            children.add(new NamedLibraryElementNode(getProject(), new NamedLibraryElement(null, libraryOrderEntry), getSettings()));
          }
        }
        else if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry) {
          final ModuleExtensionWithSdkOrderEntry sdkOrderEntry = (ModuleExtensionWithSdkOrderEntry)orderEntry;
          final Sdk sdk = sdkOrderEntry.getSdk();
          if (sdk != null) {
            if (processedSdk.contains(sdk)) continue;
            processedSdk.add(sdk);
            children.add(new NamedLibraryElementNode(getProject(), new NamedLibraryElement(null, sdkOrderEntry), getSettings()));
          }
        }
        else if(orderEntry instanceof OrderEntryWithTracking) {
          if(!orderEntry.isValid()) {
            continue;
          }
          for (OrderEntry processedCustomOrderEntry : processedCustomOrderEntries) {
            if(processedCustomOrderEntry.isEquivalentTo(orderEntry)) {
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
  public static void addLibraryChildren(final LibraryOrderEntry entry, final List<AbstractTreeNode> children, Project project, ProjectViewNode node) {
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
      if (!index.isInContent(PathUtil.getLocalFile(file))) return true;
    }
    return false;
  }


  @Override
  protected void update(PresentationData presentation) {
    presentation.setPresentableText(IdeBundle.message("node.projectview.external.libraries"));
    presentation.setIcon(AllIcons.Nodes.PpLibFolder);
  }
}
