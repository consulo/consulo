/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.intellij.ide.projectView.impl.nodes;

import com.intellij.icons.AllIcons;
import com.intellij.ide.bookmarks.Bookmark;
import com.intellij.ide.bookmarks.BookmarkManager;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.ide.projectView.impl.ProjectViewImpl;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.NavigatableWithText;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.impl.file.PsiPackageHelper;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.RowIcon;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.PathUtil;
import consulo.fileTypes.impl.VfsIconUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collection;

public class PsiDirectoryNode extends BasePsiNode<PsiDirectory> implements NavigatableWithText {
  public PsiDirectoryNode(Project project, PsiDirectory value, ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  @Override
  protected void updateImpl(PresentationData data) {
    final Project project = getProject();
    final PsiDirectory psiDirectory = getValue();
    final VirtualFile directoryFile = psiDirectory.getVirtualFile();

    final Object parentValue = getParentValue();
    if (ProjectRootsUtil.isModuleContentRoot(directoryFile, project)) {
      ProjectFileIndex fi = ProjectRootManager.getInstance(project).getFileIndex();
      Module module = fi.getModuleForFile(directoryFile);

      data.setPresentableText(directoryFile.getName());
      if (module != null) {
        if (!(parentValue instanceof Module)) {
          if (Comparing.equal(module.getName(), directoryFile.getName())) {
            data.addText(directoryFile.getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
          }
          else {
            data.addText(directoryFile.getName() + " ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
            data.addText("[" + module.getName() + "]", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
          }
        }
        else {
          data.addText(directoryFile.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        if (parentValue instanceof Module || parentValue instanceof Project) {
          final String location = FileUtil.getLocationRelativeToUserHome(directoryFile.getPresentableUrl());
          data.addText(" (" + location + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
        else if (ProjectRootsUtil.isSourceOrTestRoot(directoryFile, project)) {
          if (ProjectRootsUtil.isInTestSource(directoryFile, project)) {
            data.addText(" (test source root)", SimpleTextAttributes.GRAY_ATTRIBUTES);
          }
          else {
            data.addText(" (source root)",  SimpleTextAttributes.GRAY_ATTRIBUTES);
          }
        }

        setupIcon(data, psiDirectory);

        return;
      }
    }

    final String name = parentValue instanceof Project
                        ? psiDirectory.getVirtualFile().getPresentableUrl()
                        : BaseProjectViewDirectoryHelper.getNodeName(getSettings(), parentValue, psiDirectory);
    if (name == null) {
      setValue(null);
      return;
    }

    data.setPresentableText(name);
    if (ProjectRootsUtil.isLibraryRoot(directoryFile, project)) {
      data.setLocationString("library home");
    }
    else {
      data.setLocationString(BaseProjectViewDirectoryHelper.getLocationString(psiDirectory));
    }

    setupIcon(data, psiDirectory);
  }

  protected void setupIcon(PresentationData data, PsiDirectory psiDirectory) {
    final VirtualFile virtualFile = psiDirectory.getVirtualFile();
    final Icon icon = VfsIconUtil.getIcon(virtualFile, 0, myProject);
    data.setIcon(patchIcon(icon, virtualFile));
  }

  @Override
  public Collection<AbstractTreeNode> getChildrenImpl() {
    return BaseProjectViewDirectoryHelper.getDirectoryChildren(getValue(), getSettings(), true);
  }

  @Override
  @SuppressWarnings("deprecation")
  public String getTestPresentation() {
    return "PsiDirectory: " + getValue().getName();
  }

  public boolean isFQNameShown() {
    return BaseProjectViewDirectoryHelper.isShowFQName(getProject(), getSettings(), getParentValue(), getValue());
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    final PsiDirectory value = getValue();
    if (value == null) {
      return false;
    }

    VirtualFile directory = value.getVirtualFile();
    if (directory.getFileSystem() instanceof LocalFileSystem) {
      file = PathUtil.getLocalFile(file);
    }

    if (!VfsUtilCore.isAncestor(directory, file, false)) {
      return false;
    }

    return !FileTypeRegistry.getInstance().isFileIgnored(file);
  }

  @Override
  public VirtualFile getVirtualFile() {
    PsiDirectory directory = getValue();
    if (directory == null) return null;
    return directory.getVirtualFile();
  }

  @Override
  public boolean canRepresent(final Object element) {
    if (super.canRepresent(element)) return true;
    PsiDirectory directory = getValue();
    if (directory == null) return false;
    return BaseProjectViewDirectoryHelper.canRepresent(element, directory);
  }

  @Override
  public boolean canNavigate() {
    VirtualFile file = getVirtualFile();
    Project project = getProject();

    ProjectSettingsService service = ProjectSettingsService.getInstance(myProject);
    return file != null && ((ProjectRootsUtil.isModuleContentRoot(file, project) && service.canOpenModuleSettings()) ||
                            (ProjectRootsUtil.isSourceOrTestRoot(file, project)  && service.canOpenContentEntriesSettings()) ||
                            (ProjectRootsUtil.isLibraryRoot(file, project) && service.canOpenModuleLibrarySettings()));
  }

  @Override
  public boolean canNavigateToSource() {
    return false;
  }

  @Override
  public void navigate(final boolean requestFocus) {
    Module module = ModuleUtil.findModuleForPsiElement(getValue());
    if (module != null) {
      final VirtualFile file = getVirtualFile();
      final Project project = getProject();
      ProjectSettingsService service = ProjectSettingsService.getInstance(myProject);
      if (ProjectRootsUtil.isModuleContentRoot(file, project)) {
        service.openModuleSettings(module);
      }
      else if (ProjectRootsUtil.isLibraryRoot(file, project)) {
        final OrderEntry orderEntry = LibraryUtil.findLibraryEntry(file, module.getProject());
        if (orderEntry != null) {
          service.openLibraryOrSdkSettings(orderEntry);
        }
      }
      else {
        service.openContentEntriesSettings(module);
      }
    }
  }

  @Override
  public String getNavigateActionText(boolean focusEditor) {
    VirtualFile file = getVirtualFile();
    Project project = getProject();

    if (file != null) {
      if (ProjectRootsUtil.isModuleContentRoot(file, project) ||
          ProjectRootsUtil.isSourceOrTestRoot(file, project)) {
        return "Open Module Settings";
      }
      if (ProjectRootsUtil.isLibraryRoot(file, project)) {
        return "Open Library Settings";
      }
    }

    return null;
  }

  @Override
  public int getWeight() {
    final ProjectView projectView = ProjectView.getInstance(myProject);
    if (projectView instanceof ProjectViewImpl && !((ProjectViewImpl)projectView).isFoldersAlwaysOnTop()) {
      return 20;
    }
    return isFQNameShown() ? 70 : 0;
  }

  @Override
  public String getTitle() {
    final PsiDirectory directory = getValue();
    if (directory != null) {
      return PsiPackageHelper.getInstance(getProject()).getQualifiedName(directory, true);
    }
    return super.getTitle();
  }

  protected Icon patchIcon(Icon original, VirtualFile file) {
    Icon icon = original;

    final Bookmark bookmarkAtFile = BookmarkManager.getInstance(myProject).findFileBookmark(file);
    if (bookmarkAtFile != null) {
      final RowIcon composite = new RowIcon(2, RowIcon.Alignment.CENTER);
      composite.setIcon(icon, 0);
      composite.setIcon(bookmarkAtFile.getIcon(), 1);
      icon = composite;
    }

    if (!file.isWritable()) {
      icon = LayeredIcon.create(icon, AllIcons.Nodes.Locked);
    }

    if (file.is(VFileProperty.SYMLINK)) {
      icon = LayeredIcon.create(icon, AllIcons.Nodes.Symlink);
    }

    return icon;
  }

  @Override
  public String getQualifiedNameSortKey() {
    final PsiPackageHelper factory = PsiPackageHelper.getInstance(getProject());
    return factory.getQualifiedName(getValue(), true);
  }

  @Override
  public int getTypeSortWeight(final boolean sortByType) {
    return 3;
  }

  @Override
  public boolean shouldDrillDownOnEmptyElement() {
    return true;
  }

  @Override
  public boolean isAlwaysShowPlus() {
    final VirtualFile file = getVirtualFile();
    return file == null || file.getChildren().length > 0;
  }
}
